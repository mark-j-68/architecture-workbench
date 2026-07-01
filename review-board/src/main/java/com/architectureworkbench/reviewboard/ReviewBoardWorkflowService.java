package com.architectureworkbench.reviewboard;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.audit.ReviewCompleted;
import com.architectureworkbench.audit.ReviewRequested;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReviewBoardWorkflowService {
    private final AuditSink auditSink;

    public ReviewBoardWorkflowService(AuditSink auditSink) {
        this.auditSink = Objects.requireNonNull(auditSink, "auditSink");
    }

    public ReviewBoardSession openSession(
            String workspaceId,
            CorrelationId correlationId,
            List<Recommendation> recommendationCandidates,
            List<ProposedArchitectureChange> proposedChanges,
            String actorRef
    ) {
        ReviewBoardSession session = new ReviewBoardSession(
                null,
                workspaceId,
                correlationId,
                ReviewBoardSessionStatus.OPEN,
                recommendationCandidates,
                proposedChanges,
                List.of(),
                List.of(),
                null,
                null,
                null
        );
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.REVIEW_REQUESTED,
                workspaceId,
                ArchitectureEventSource.REVIEW_BOARD_SERVICE,
                Actor.human(actorRef),
                CausationId.newId("open-review-board-session"),
                correlationId,
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                new ReviewRequested(session.sessionId().value(), 0, false).payload(),
                null
        ));
        return session;
    }

    public ReviewBoardSession addParticipant(ReviewBoardSession session, ReviewBoardParticipant participant) {
        requireOpen(session);
        List<ReviewBoardParticipant> participants = new ArrayList<>(session.participants());
        participants.add(participant);
        return copy(session, session.status(), participants, session.votes(), session.decision(), session.closedAt());
    }

    public ReviewBoardSession recordVote(ReviewBoardSession session, ReviewBoardVote vote) {
        requireOpen(session);
        if (session.participants().stream().noneMatch(participant -> participant.participantId().equals(vote.participantId()))) {
            throw new IllegalArgumentException("Vote participant is not part of the review board session: " + vote.participantId());
        }
        List<ReviewBoardVote> votes = new ArrayList<>(session.votes());
        votes.add(vote);
        return copy(session, session.status(), session.participants(), votes, session.decision(), session.closedAt());
    }

    public ReviewBoardSession closeSession(ReviewBoardSession session, String actorRef) {
        requireOpen(session);
        ReviewBoardDecision decision = deriveReviewBoardDecision(session);
        ReviewBoardSession closed = copy(session, ReviewBoardSessionStatus.CLOSED, session.participants(), session.votes(), decision, Instant.now());
        auditSink.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.REVIEW_COMPLETED,
                session.workspaceId(),
                ArchitectureEventSource.REVIEW_BOARD_SERVICE,
                Actor.human(actorRef),
                CausationId.newId("close-review-board-session"),
                session.correlationId(),
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                new ReviewCompleted(session.sessionId().value(), session.votes().size(), "", false).payload(),
                null
        ));
        return closed;
    }

    public ReviewBoardDecision deriveReviewBoardDecision(ReviewBoardSession session) {
        if (session.votes().isEmpty()) {
            return new ReviewBoardDecision(
                    ReviewBoardDecisionType.REQUEST_FURTHER_REVIEW,
                    "No review board votes were recorded.",
                    List.of(),
                    null
            );
        }
        long approvals = count(session, ReviewBoardVoteType.APPROVE);
        long conditionalApprovals = count(session, ReviewBoardVoteType.APPROVE_WITH_CONDITIONS);
        long rejections = count(session, ReviewBoardVoteType.REJECT);
        long deferrals = count(session, ReviewBoardVoteType.DEFER);
        long evidenceRequests = count(session, ReviewBoardVoteType.REQUEST_MORE_EVIDENCE);

        if (evidenceRequests > 0) {
            return new ReviewBoardDecision(
                    ReviewBoardDecisionType.REQUEST_FURTHER_DISCOVERY,
                    "At least one participant requested more evidence before deciding.",
                    rationales(session, ReviewBoardVoteType.REQUEST_MORE_EVIDENCE),
                    null
            );
        }
        if (rejections > 0 && approvals == 0 && conditionalApprovals == 0) {
            return new ReviewBoardDecision(
                    ReviewBoardDecisionType.REJECT_PROPOSED_CHANGE,
                    "Review board rejected the recommendation candidate or proposed change.",
                    rationales(session, ReviewBoardVoteType.REJECT),
                    null
            );
        }
        if (approvals == session.votes().size()) {
            return new ReviewBoardDecision(
                    ReviewBoardDecisionType.ACCEPT_PROPOSED_CHANGE,
                    "Review board unanimously approved the proposed change.",
                    List.of(),
                    null
            );
        }
        if (conditionalApprovals > 0 && rejections == 0 && deferrals == 0) {
            return new ReviewBoardDecision(
                    ReviewBoardDecisionType.ACCEPT_PROPOSED_CHANGE,
                    "Review board approved with conditions.",
                    rationales(session, ReviewBoardVoteType.APPROVE_WITH_CONDITIONS),
                    null
            );
        }
        return new ReviewBoardDecision(
                ReviewBoardDecisionType.DEFER_PROPOSED_CHANGE,
                "Review board votes were conflicting or inconclusive.",
                rationales(session, ReviewBoardVoteType.DEFER),
                null
        );
    }

    private static long count(ReviewBoardSession session, ReviewBoardVoteType type) {
        return session.votes().stream().filter(vote -> vote.voteType() == type).count();
    }

    private static List<String> rationales(ReviewBoardSession session, ReviewBoardVoteType type) {
        return session.votes().stream()
                .filter(vote -> vote.voteType() == type)
                .map(ReviewBoardVote::rationale)
                .filter(rationale -> !rationale.isBlank())
                .toList();
    }

    private static void requireOpen(ReviewBoardSession session) {
        if (session.status() != ReviewBoardSessionStatus.OPEN) {
            throw new IllegalStateException("Review board session is not open.");
        }
    }

    private static ReviewBoardSession copy(
            ReviewBoardSession session,
            ReviewBoardSessionStatus status,
            List<ReviewBoardParticipant> participants,
            List<ReviewBoardVote> votes,
            ReviewBoardDecision decision,
            Instant closedAt
    ) {
        return new ReviewBoardSession(
                session.sessionId(),
                session.workspaceId(),
                session.correlationId(),
                status,
                session.recommendationCandidates(),
                session.proposedChanges(),
                participants,
                votes,
                decision,
                session.openedAt(),
                closedAt
        );
    }
}
