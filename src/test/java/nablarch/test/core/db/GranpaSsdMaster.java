package nablarch.test.core.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GRANPA", schema = "SSD_MASTER")
public class GranpaSsdMaster {
    
    public GranpaSsdMaster() {
    };
    
	public GranpaSsdMaster(String myid) {
		this.myid = myid;
	}

	@Id
    @Column(name = "MYID", length = 1, nullable = false)
    public String myid;
}