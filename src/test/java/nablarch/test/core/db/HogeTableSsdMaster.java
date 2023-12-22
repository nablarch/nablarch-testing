package nablarch.test.core.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;


/**
 * HOGE_TABLE
 */
@Entity
@Table(name = "HOGE_TABLE", schema = "SSD_MASTER")
public class HogeTableSsdMaster {
    
    public HogeTableSsdMaster() {
    };
    
    public HogeTableSsdMaster(String pkCol1, Long pkCol2, String varchar2Col,
			Long numberCol, BigDecimal numberCol2, Date dateCol,
			Timestamp timestampCol, String nullCol, char[] clobCol,
			byte[] blobCol) {
		this.pkCol1 = pkCol1;
		this.pkCol2 = pkCol2;
		this.varchar2Col = varchar2Col;
		this.numberCol = numberCol;
		this.numberCol2 = numberCol2;
		this.dateCol = dateCol;
		this.timestampCol = timestampCol;
		this.nullCol = nullCol;
		this.clobCol = clobCol;
		this.blobCol = blobCol;
	}

	@Id
    @Column(name = "PK_COL1", length = 5, nullable = false)
    public String pkCol1;
    
    @Id
    @Column(name = "PK_COL2", length = 2, nullable = false)
    public Long pkCol2;
    
    @Column(name = "VARCHAR2_COL", length = 20)
    public String varchar2Col;
    
    @Column(name = "NUMBER_COL", length = 10, nullable = false)
    public Long numberCol;
    
    @Column(name = "NUMBER_COL2", precision = 10, scale = 3, nullable = false)
    public BigDecimal numberCol2;
    
    @Column(name = "DATE_COL", nullable = false)
    public Date dateCol;
    
    @Column(name = "TIMESTAMP_COL", nullable = false)
    public Timestamp timestampCol;
    
    @Column(name = "NULL_COL", length = 5)
    public String nullCol;
    
    @Lob
    @Column(name = "CLOB_COL")
    public char[] clobCol;
    
    @Lob
    @Column(name = "BLOB_COL")
    public byte[] blobCol;
}
