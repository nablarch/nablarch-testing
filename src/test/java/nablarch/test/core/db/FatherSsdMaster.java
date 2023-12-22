package nablarch.test.core.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "FATHER", schema = "SSD_MASTER")
public class FatherSsdMaster {
	
    public FatherSsdMaster() {
    };

	public FatherSsdMaster(String myid, GranpaSsdMaster granpa) {
		this.myid = myid;
		this.granpa = granpa;
	}

	@Id
    @Column(name = "MYID", length = 1, nullable = false)
    public String myid;
	
	@ManyToOne
	@JoinColumn(name="MY_PARENT", referencedColumnName="MYID")
    public GranpaSsdMaster granpa;
}