package edu.esi.ds.esientradas.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.esi.ds.esientradas.dao.EmailQueueDao;
import edu.esi.ds.esientradas.model.EmailQueue;
import edu.esi.ds.esientradas.model.EmailStatus;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    @Value("${email.external-url:http://localhost:8081/external/sendTicket}")
    private String externalUrl;

    @Value("${email.max-attempts:5}")
    private int maxAttempts;

    @Autowired
    private EmailQueueDao queueDao;

    private final RestTemplate rest = new RestTemplate();

    @Transactional
    public EmailQueue enqueue(String to, String subject, String html, String reference) {
        EmailQueue q = new EmailQueue();
        q.setToAddress(to);
        q.setSubject(subject);
        q.setBodyHtml(html);
        q.setReference(reference);
        q.setStatus(EmailStatus.PENDING);
        q.setAttempts(0);
        q = this.queueDao.save(q);
        return q;
    }

    @Transactional
    public void enqueueAndTrySend(String to, String subject, String html, String reference) {
        EmailQueue q = this.enqueue(to, subject, html, reference);
        try {
            this.trySendNow(q);
        } catch (Exception e) {
            log.warn("Initial send failed, queued for retry: {}", e.getMessage());
        }
    }

    @Transactional
    public void trySendNow(EmailQueue q) {
        try {
            var body = java.util.Map.of("to", q.getToAddress(), "subject", q.getSubject(), "html", q.getBodyHtml());
            ResponseEntity<String> resp = rest.postForEntity(this.externalUrl, body, String.class);
            q.setLastAttempt(System.currentTimeMillis());
            q.setAttempts(q.getAttempts() + 1);
            if (resp.getStatusCode().is2xxSuccessful()) {
                q.setStatus(EmailStatus.SENT);
            } else {
                q.setStatus(EmailStatus.PENDING);
            }
            this.queueDao.save(q);
        } catch (RestClientException ex) {
            q.setLastAttempt(System.currentTimeMillis());
            q.setAttempts(q.getAttempts() + 1);
            if (q.getAttempts() >= this.maxAttempts) {
                q.setStatus(EmailStatus.FAILED);
            } else {
                q.setStatus(EmailStatus.PENDING);
            }
            this.queueDao.save(q);
            throw ex;
        }
    }

    @Transactional
    public void processQueueById(Long id) {
        EmailQueue q = this.queueDao.findById(id).orElse(null);
        if (q == null) return;
        if (q.getStatus() == EmailStatus.SENT) return;
        try {
            this.trySendNow(q);
        } catch (Exception e) {
            log.warn("Processing queued email {} failed: {}", id, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${email.retry.ms:60000}")
    public void retryPending() {
        try {
            List<EmailQueue> pending = this.queueDao.findByStatusAndAttemptsLessThan(EmailStatus.PENDING, this.maxAttempts);
            for (EmailQueue q : pending) {
                try {
                    this.trySendNow(q);
                } catch (Exception e) {
                    log.warn("Retry failed for email id {} attempts {}: {}", q.getId(), q.getAttempts(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error while retrying pending emails: {}", e.getMessage());
        }
    }
}
