package nablarch.test.core.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.validation.validator.Required;
import nablarch.test.Trap;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * {@link EntityTestSupport}のテストクラス。
 *
 * @author hisaaki sioiri
 */
public class EntityTestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** テスト対象 */
    private EntityTestSupport support = new EntityTestSupport(getClass());

    private static final String[][] MESSAGES = {{"MSG00010", "ja", "message", "en", "message"}};

    /** {@link EntityTestSupport#testSetterAndGetter(java.lang.Class, java.lang.String, java.lang.String)}のテスト。 */
    @Test
    public void testTestSetterAntGetter() {
        support.testSetterAndGetter(HogeEntity.class, "test1", "entity");
        support.testSetterAndGetter(HogeChild.class, "test1", "entity");    // サブクラスでも動作する
        support.testSetterAndGetter(HogeChild2.class, "test1", "constructor");   // デフォルトコンストラクタのみでも動作する
        Assert.assertTrue(true);
    }

    /** {@link EntityTestSupport#testConstructorAndGetter(Class, String, String)} のテスト。 */
    @Test
    public void testTestConstructorAndGetter() {
        support.testConstructorAndGetter(HogeEntity.class, "test1", "constructor");
        support.testConstructorAndGetter(HogeChild.class, "test1", "constructor");    // サブクラスでも動作する
        Assert.assertTrue(true);
    }

    /**
     * {@link EntityTestSupport#testValidateAndConvert(Class, String, String)}のテスト。
     */
    @Test
    public void testTestValidateAndConvert() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                          .setMessages(MESSAGES);

        support.testValidateAndConvert(FugaEntity.class, "testValidateAndConvert", null);
    }

    @Test
    public void testRequiredColumnAbsent() throws Exception {
        new Trap("必須カラムがない場合には、例外が発生すること。") {
            @Override
            protected void shouldFail() throws Exception {
                support.testValidateAndConvert(FugaEntity.class, "testRequiredColumnAbsent", null);
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("TestCase does NOT have required columns.");
    }

    /** テストケース表とパラメータ表とで行数が異なる場合、例外が発生すること。 */
    @Test
    public void testDataSizeDiffer() {
        new Trap("testCasesよりparamsのほうがデータ数が多い") {
            @Override
            protected void shouldFail() throws Exception {
                support.testValidateAndConvert(FugaEntity.class,
                        "testValidateAndConvertFail", null);
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("'testCases' has 4 line(s). but 'params' has 3.");
    }

    /** パラメータ表データが見つからない場合、例外が発生すること */
    @Test
    public void testValidateParamsNotFound() {
        new Trap("パラメータ表データが見つからない場合、例外が発生すること") {
            @Override
            protected void shouldFail() throws Exception {
                support.testValidateAndConvert(FugaEntity.class, "testValidateParamsNotFound", null);
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("data [params] not found in sheet [testValidateParamsNotFound].");
    }

    /** テストショット表データが見つからない場合、例外が発生すること */
    @Test
    public void testValidateTestShotsNotFound() {
        new Trap("テストショット表データが見つからない場合、例外が発生すること") {
            @Override
            protected void shouldFail() throws Exception {
                support.testValidateAndConvert(FugaEntity.class, "testValidateTestShotsNotFound", null);
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("data [testShots] not found in sheet [testValidateTestShotsNotFound].");
    }

    /** 指定したIDのデータが見つからない場合、例外が発生すること */
    @Test
    public void testSetterAndGetterNotFound() {
        new Trap("指定したIDのデータが見つからない場合、例外が発生すること") {
            @Override
            protected void shouldFail() throws Exception {
                support.testSetterAndGetter(FugaEntity.class, "test1", "not-exists");
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("data [not-exists] not found in sheet [test1].");
    }

    /** 指定したIDのデータが見つからない場合、例外が発生すること */
    @Test
    public void testAssertGetterMethodDataNotFound() {
        new Trap("指定したIDのデータが見つからない場合、例外が発生すること") {
            @Override
            protected void shouldFail() throws Exception {
                support.assertGetterMethod("test1", "not-exist", null);
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("data [not-exist] not found in sheet [test1].");
    }

    /**
     * コンストラクタがプライベートの場合やコンストラクタ中で例外が発生した場合は、例外が発生すること。
     */
    @Test
    public void testPrivateConstructorEntity() {
        new Trap("引数なしコンストラクタがプライベートの場合") {
            @Override
            protected void shouldFail() throws Exception {
                support.testConstructorAndGetter(PrivateConstructorEntity.class, "testPrivateConstructorEntity",
                        "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("failed to instantiate the class.");
        new Trap("Map引数のコンストラクタがプライベートの場合") {
            @Override
            protected void shouldFail() throws Exception {
                support.testConstructorAndGetter(PrivateMapConstructorEntity.class, "testPrivateConstructorEntity",
                        "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("failed to instantiate the class.");
        new Trap("Map引数のコンストラクタで例外が発生する場合") {
            @Override
            protected void shouldFail() throws Exception {
                support.testConstructorAndGetter(ExceptionMapConstructorEntity.class, "testPrivateConstructorEntity",
                        "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("failed to instantiate the class.");
    }

    /**
     * Excelシートで記載されている値が、プロパティの型にキャストできない場合。
     */
    @Test
    public void testCastFailure() throws Exception {
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                support.testConstructorAndGetter(HogeEntity.class, "testCastFailure", "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("data cast error")
         .whichMessageContains("property name = [updateCount]");
    }

    /**
     * Excelシートで記載されている値が、エラーになった場合のログ確認。
     */
    @Test
    public void testInvalidInputConstructorAndGetter() throws Exception {
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                support.testConstructorAndGetter(HogeEntity.class, "testInvalidInput", "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("getter is not found. getter name=[].",
                 "please make sure \"name\" column is valid.",
                 "sheet=[testInvalidInput] id=[entity] row=[2]."
         );
    }

    /**
     * Excelシートで記載されている値が、エラーになった場合のログ確認。
     */
    @Test
    public void testInvalidInputSetterAndGetter() throws Exception {
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                support.testSetterAndGetter(HogeEntity.class, "testInvalidInput", "entity");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("getter is not found. getter name=[] sheet=[testInvalidInput] id=[entity] row=[2].");
    }


    public static class PrivateConstructorEntity {

        private String userName;

        private PrivateConstructorEntity() {
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class PrivateMapConstructorEntity {

        private String userName;

        private PrivateMapConstructorEntity(Map<String, Object> map) {
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class ExceptionMapConstructorEntity {

        private String userName;

        public ExceptionMapConstructorEntity(Map<String, Object> map) {
            throw new RuntimeException("exception occurred.");
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class FugaEntity {

        private String userName;

        public FugaEntity(Map<String, Object> params) {
            userName = (String) params.get("userName");
        }

        public String getUserName() {
            return userName;
        }

        @Required
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class HogeEntity {

        private String userName;

        private String[] userNames;

        private Integer updateCount;

        private Integer[] updateCounts;

        private BigDecimal rate;

        private BigDecimal[] rates;

        private Long amt;

        private Long[] amts;

        private String hoge2;

        private java.sql.Date newDate;

        private java.sql.Date[] newDates;

        private Timestamp newTimestamp;

        private Timestamp[] newTimestamps;

        private String postNoOya;

        private String postNoKo;

        public HogeEntity() {
        }

        public HogeEntity(Map<String, Object> params) {
            userName = (String) params.get("userName");
            userNames = (String[]) params.get("userNames");
            updateCount = (Integer) params.get("updateCount");
            updateCounts = (Integer[]) params.get("updateCounts");
            rate = (BigDecimal) params.get("rate");
            rates = (BigDecimal[]) params.get("rates");
            amt = (Long) params.get("amt");
            amts = (Long[]) params.get("amts");
            hoge2 = (String) params.get("hoge2");
            newDate = (java.sql.Date) params.get("newDate");
            newDates = (java.sql.Date[]) params.get("newDates");
            newTimestamp = (Timestamp) params.get("newTimestamp");
            newTimestamps = (Timestamp[]) params.get("newTimestamps");
            postNoOya = (String) params.get("postNoOya");
            postNoKo = (String) params.get("postNoKo");

        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public Integer getUpdateCount() {
            return updateCount;
        }

        public void setUpdateCount(Integer updateCount) {
            this.updateCount = updateCount;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public String[] getUserNames() {
            return userNames;
        }

        public void setUserNames(String[] userNames) {
            this.userNames = userNames;
        }

        public Integer[] getUpdateCounts() {
            return updateCounts;
        }

        public void setUpdateCounts(Integer[] updateCounts) {
            this.updateCounts = updateCounts;
        }

        public BigDecimal[] getRates() {
            return rates;
        }

        public void setRates(BigDecimal[] rates) {
            this.rates = rates;
        }

        public Long getAmt() {
            return amt;
        }

        public void setAmt(Long amt) {
            this.amt = amt;
        }

        public Long[] getAmts() {
            return amts;
        }

        public void setAmts(Long[] amts) {
            this.amts = amts;
        }

        public String getHoge1() {
            return userName;
        }

        public void setHoge2(String hoge2) {
            this.hoge2 = hoge2;
        }

        public String getHoge2Prefix() {
            return hoge2.substring(0, 4);
        }

        public String getHoge2Sufix() {
            return hoge2.substring(4);
        }

        public Date getNewDate() {
            return newDate;
        }

        public void setNewDate(Date newDate) {
            this.newDate = newDate;
        }

        public Date[] getNewDates() {
            return newDates;
        }

        public void setNewDates(Date[] newDates) {
            this.newDates = newDates;
        }

        public Timestamp getNewTimestamp() {
            return newTimestamp;
        }

        public void setNewTimestamp(Timestamp newTimestamp) {
            this.newTimestamp = newTimestamp;
        }

        public Timestamp[] getNewTimestamps() {
            return newTimestamps;
        }

        public void setNewTimestamps(Timestamp[] newTimestamps) {
            this.newTimestamps = newTimestamps;
        }

        public void setPostNoOya(String postNoOya) {
            this.postNoOya = postNoOya;
        }

        public void setPostNoKo(String postNoKo) {
            this.postNoKo = postNoKo;
        }

        public String getPostNo() {
            return postNoOya + postNoKo;
        }
    }

    /** サブクラスでもテストできることを確認する為のクラス */
    public static class HogeChild extends HogeEntity {

        public HogeChild(Map<String, Object> params) {
            super(params);
        }
    }

    public static class HogeChild2 extends HogeEntity {

        public HogeChild2() {
        }
    }
}

