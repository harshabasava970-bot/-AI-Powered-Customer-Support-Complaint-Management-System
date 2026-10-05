package com.supportportal.repository;

import com.supportportal.entity.Complaint;
import com.supportportal.entity.ComplaintPriority;
import com.supportportal.entity.ComplaintStatus;
import com.supportportal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>,
        JpaSpecificationExecutor<Complaint> {

    // Customer-specific queries
    Page<Complaint> findByCustomer(User customer, Pageable pageable);

    List<Complaint> findByCustomer(User customer);

    // Agent-specific queries
    Page<Complaint> findByAssignedAgent(User agent, Pageable pageable);

    List<Complaint> findByAssignedAgent(User agent);

    // Status-based queries
    List<Complaint> findByStatus(ComplaintStatus status);

    long countByStatus(ComplaintStatus status);

    long countByPriority(ComplaintPriority priority);

    // Agent workload
    long countByAssignedAgent(User agent);

    long countByAssignedAgentAndStatus(User agent, ComplaintStatus status);

    // Category stats
    @Query("SELECT c.category.name, COUNT(c) FROM Complaint c GROUP BY c.category.name")
    List<Object[]> countByCategory();

    // Status stats
    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    // Agent workload summary
    @Query("SELECT c.assignedAgent, COUNT(c) FROM Complaint c WHERE c.assignedAgent IS NOT NULL GROUP BY c.assignedAgent")
    List<Object[]> countByAssignedAgent();

    // Resolved complaints for avg time / feedback
    @Query("SELECT AVG(f.rating) FROM ComplaintFeedback f")
    Double findAverageRating();

    @Query("SELECT COUNT(f) FROM ComplaintFeedback f")
    long countFeedback();

    // Search support
    @Query("""
            SELECT c FROM Complaint c
            WHERE (:customerId IS NULL OR c.customer.id = :customerId)
              AND (:status IS NULL OR c.status = :status)
              AND (:priority IS NULL OR c.priority = :priority)
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:agentId IS NULL OR c.assignedAgent.id = :agentId)
              AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
              AND (:toDate IS NULL OR c.createdAt <= :toDate)
            ORDER BY c.createdAt DESC
            """)
    Page<Complaint> searchComplaints(
            @Param("customerId") Long customerId,
            @Param("status") ComplaintStatus status,
            @Param("priority") ComplaintPriority priority,
            @Param("categoryId") Long categoryId,
            @Param("agentId") Long agentId,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    // Overdue: NEW/ASSIGNED with no update for > 48h
    @Query("SELECT c FROM Complaint c WHERE c.status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS') AND c.updatedAt < :cutoff")
    List<Complaint> findOverdueComplaints(@Param("cutoff") LocalDateTime cutoff);
}
