package nablarch.test.core.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


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