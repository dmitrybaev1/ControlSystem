import java.util.*
import javax.persistence.*

@Entity
@Table(name = "Pass_request")
class PassRequest (@Id @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "Pass_req_seq") @SequenceGenerator
    (name = "Pass_req_seq",sequenceName = "pass_request_seq",allocationSize = 1) var id: Int? = null,@ManyToOne @JoinColumn
    (name = "employee_id", referencedColumnName = "id")  val employeeId: Employee, @Temporal(TemporalType.TIMESTAMP)
    @Column(name="datetime_msk") val dateTimeMsk: Date,val approved: Boolean)