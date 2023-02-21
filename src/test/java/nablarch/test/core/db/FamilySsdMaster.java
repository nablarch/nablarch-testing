package nablarch.test.core.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "FAMILY", schema = "SSD_MASTER")
public class FamilySsdMaster {
    
    public FamilySsdMaster() {
    };

    public FamilySsdMaster(String famid, FatherSsdMaster father, DaughterSsdMaster daughter) {
        this.famid = famid;
        this.father = father;
        this.daughter = daughter;
    }

    @Id
    @Column(name = "FAMID", length = 1, nullable = false)
    public String famid;

    @ManyToOne
    @JoinColumn(name="PARENT", referencedColumnName="MYID")
    public FatherSsdMaster father;

    @ManyToOne
    @JoinColumn(name="CHILD2", referencedColumnName="MYID")
    public DaughterSsdMaster daughter;
}
