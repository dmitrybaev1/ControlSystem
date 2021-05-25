import javax.persistence.*

@Entity
@Table(name = "Employee")
class Employee (@Id @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "Emp_seq") @SequenceGenerator
    (name = "Emp_seq",sequenceName = "employee_seq",allocationSize = 1) var id: Int? = null, val name: String,val position: String,
                @Column(name = "card_id") val cardId: String)