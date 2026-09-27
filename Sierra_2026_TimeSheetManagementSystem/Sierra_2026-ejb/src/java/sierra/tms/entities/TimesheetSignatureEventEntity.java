package sierra.tms.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import sierra.tms.utils.enums.TimesheetActorCapacity;
import sierra.tms.utils.enums.TimesheetAuthenticationMethod;
import sierra.tms.utils.enums.TimesheetSignatureAction;

@Entity
@Table(name = "TimesheetSignatureEvent")
public class TimesheetSignatureEventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timesheet_id", nullable = false, updatable = false)
    private TimesheetEntity timesheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_person_id", nullable = false, updatable = false)
    private PersonEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false)
    private TimesheetSignatureAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_capacity", nullable = false, updatable = false)
    private TimesheetActorCapacity actorCapacity;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_method", nullable = false, updatable = false)
    private TimesheetAuthenticationMethod authenticationMethod;

    protected TimesheetSignatureEventEntity() {
    }

    public TimesheetSignatureEventEntity(
            TimesheetEntity timesheet,
            PersonEntity actor,
            TimesheetSignatureAction action,
            TimesheetActorCapacity actorCapacity,
            LocalDateTime occurredAt,
            TimesheetAuthenticationMethod authenticationMethod) {
        this.timesheet = timesheet;
        this.actor = actor;
        this.action = action;
        this.actorCapacity = actorCapacity;
        this.occurredAt = occurredAt;
        this.authenticationMethod = authenticationMethod;
    }

    public Long getId() {
        return id;
    }

    public TimesheetEntity getTimesheet() {
        return timesheet;
    }

    public PersonEntity getActor() {
        return actor;
    }

    public TimesheetSignatureAction getAction() {
        return action;
    }

    public TimesheetActorCapacity getActorCapacity() {
        return actorCapacity;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public TimesheetAuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }
}
