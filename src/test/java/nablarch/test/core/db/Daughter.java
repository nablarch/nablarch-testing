package nablarch.test.core.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "DAUGHTER")
public class Daughter {
	
    public Daughter() {
    };

	public Daughter(String myid, Father father) {
		this.myid = myid;
		this.father = father;
	}

	@Id
    @Column(name = "MYID", length = 1, nullable = false)
    public String myid;
	
	@ManyToOne
	@JoinColumn(name="MY_PARENT", referencedColumnName="MYID")
    public Father father;
}