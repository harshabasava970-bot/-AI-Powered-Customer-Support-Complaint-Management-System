package com.supportportal.repository;

import com.supportportal.entity.Complaint;
import com.supportportal.entity.ComplaintFeedback;
import com.supportportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintFeedbackRepository extends JpaRepository<ComplaintFeedback, Long> {

    Optional<ComplaintFeedback> findByComplaint(Complaint complaint);

    boolean existsByComplaint(Complaint complaint);

    @Query("SELECT AVG(f.rating) FROM ComplaintFeedback f JOIN f.complaint c WHERE c.assignedAgent = :agent")
    Double findAverageRatingByAgent(@Param("agent") User agent);

    @Query("SELECT COUNT(f) FROM ComplaintFeedback f JOIN f.complaint c WHERE c.assignedAgent = :agent")
    long countByAgent(@Param("agent") User agent);

    List<ComplaintFeedback> findTop10ByOrderBySubmittedAtDesc();
}
