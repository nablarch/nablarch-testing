package nablarch.test.core.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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