package project.hongik_hospital.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import static javax.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor
public class TreatmentDate {

    @Id
    @GeneratedValue
    @Column(name = "treatmentdate_id")
    private Long id;

    private int month;
    private int date;
    private int hour;
    private int minute;

    public TreatmentDate(int month, int date, int hour, int minute) {
        this.month = month;
        this.date = date;
        this.hour = hour;
        this.minute = minute;
    }

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    //비교 메서드
    public void compare(TreatmentDate treatmentDate) {
        if (this.hour == treatmentDate.hour ||
                this.date == treatmentDate.hour ||
                this.hour == treatmentDate.hour ||
                this.minute == treatmentDate.minute)
        {
            throw new IllegalStateException("오류! 이미 예약된 시간입니다.");
        }
    }
}
