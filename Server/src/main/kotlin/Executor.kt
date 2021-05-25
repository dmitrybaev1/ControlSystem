import javax.persistence.*

@Entity
@Table(name = "Executor")
class Executor(@Id @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "Exec_seq") @SequenceGenerator
    (name = "Exec_seq",sequenceName = "executor_seq",allocationSize = 1) var id: Int? = null, @Column(name = "username")
            val username: String, @Column(name = "password") val password: String,
               @Column(name = "admin") val admin: Boolean)