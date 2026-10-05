package com.supportportal.service;

import com.supportportal.dto.AgentStatsDto;
import com.supportportal.dto.DashboardStatsDto;
import com.supportportal.entity.ComplaintPriority;
import com.supportportal.entity.ComplaintStatus;
import com.supportportal.entity.Role;
import com.supportportal.entity.User;
import com.supportportal.repository.ComplaintFeedbackRepository;
import com.supportportal.repository.ComplaintRepository;
import com.supportportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public DashboardStatsDto getDashboardStats() {

        // Counts by status
        long total      = complaintRepository.count();
        long newC       = complaintRepository.countByStatus(ComplaintStatus.NEW);
        long assigned   = complaintRepository.countByStatus(ComplaintStatus.ASSIGNED);
        long inProgress = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long waiting    = complaintRepository.countByStatus(ComplaintStatus.WAITING_FOR_CUSTOMER);
        long resolved   = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long closed     = complaintRepository.countByStatus(ComplaintStatus.CLOSED);

        // Counts by priority
        long critical = complaintRepository.countByPriority(ComplaintPriority.CRITICAL);
        long high     = complaintRepository.countByPriority(ComplaintPriority.HIGH);
        long medium   = complaintRepository.countByPriority(ComplaintPriority.MEDIUM);
        long low      = complaintRepository.countByPriority(ComplaintPriority.LOW);

        // By category
        Map<String, Long> byCategory = new LinkedHashMap<>();
        complaintRepository.countByCategory()
                .forEach(row -> byCategory.put((String) row[0], (Long) row[1]));

        // By status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        complaintRepository.countGroupByStatus()
                .forEach(row -> byStatus.put(row[0].toString(), (Long) row[1]));

        // Agent workload
        Map<String, Long> workload = new LinkedHashMap<>();
        complaintRepository.countByAssignedAgent()
                .forEach(row -> {
                    User agent = (User) row[0];
                    workload.put(agent.getFullName(), (Long) row[1]);
                });

        // Satisfaction
        Double avgRating   = complaintRepository.findAverageRating();
        long feedbackCount = complaintRepository.countFeedback();

        // User counts
        long totalAgents    = userRepository.countByRole(Role.RoleName.ROLE_AGENT);
        long totalCustomers = userRepository.countByRole(Role.RoleName.ROLE_CUSTOMER);

        return DashboardStatsDto.builder()
                .totalComplaints(total)
                .newComplaints(newC)
                .assignedComplaints(assigned)
                .inProgressComplaints(inProgress)
                .waitingComplaints(waiting)
                .resolvedComplaints(resolved)
                .closedComplaints(closed)
                .criticalComplaints(critical)
                .highComplaints(high)
                .mediumComplaints(medium)
                .lowComplaints(low)
                .complaintsByCategory(byCategory)
                .complaintsByStatus(byStatus)
                .agentWorkload(workload)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                .totalFeedbackCount(feedbackCount)
                .totalAgents(totalAgents)
                .totalCustomers(totalCustomers)
                .build();
    }

    public List<AgentStatsDto> getAgentStats() {
        List<User> agents = userRepository.findAllByRole(Role.RoleName.ROLE_AGENT);
        return agents.stream().map(agent -> {
            long totalAssigned  = complaintRepository.countByAssignedAgent(agent);
            long inProg         = complaintRepository.countByAssignedAgentAndStatus(agent, ComplaintStatus.IN_PROGRESS);
            long res            = complaintRepository.countByAssignedAgentAndStatus(agent, ComplaintStatus.RESOLVED);
            long cls            = complaintRepository.countByAssignedAgentAndStatus(agent, ComplaintStatus.CLOSED);
            long wait           = complaintRepository.countByAssignedAgentAndStatus(agent, ComplaintStatus.WAITING_FOR_CUSTOMER);
            Double avgRating    = feedbackRepository.findAverageRatingByAgent(agent);
            long feedbackCount  = feedbackRepository.countByAgent(agent);

            return AgentStatsDto.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getFullName())
                    .agentEmail(agent.getEmail())
                    .totalAssigned(totalAssigned)
                    .inProgress(inProg)
                    .resolved(res)
                    .closed(cls)
                    .waitingForCustomer(wait)
                    .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                    .feedbackCount(feedbackCount)
                    .build();
        }).collect(Collectors.toList());
    }
}
