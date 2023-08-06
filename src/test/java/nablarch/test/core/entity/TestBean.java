package nablarch.test.core.entity;

import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.Required;
import nablarch.core.validation.ee.SystemChar;

/**
 * 単項目バリデーションのテスト用エンティティクラス
 * @author T.Kawasaki
 */
public class TestBean {

    @Length.List({
            @Length(max = 50),
            @Length(max = 10, groups = Test1.class)
    })
    @SystemChar(charsetDef = "半角数字")
    private String numberMax;

    @Length(max = 50, min = 20)
    @SystemChar(charsetDef = "半角数字")
    private String numberMaxMin;

    @Length(max = 10, min = 10)
    @SystemChar(charsetDef = "半角数字")
    private String numberFixed;

    @Required
    @Length(max = 5, min = 5)
    @SystemChar(charsetDef = "半角数字")
    private String numberProhibitEmpty;

    public String getNumberMax() {
        return numberMax;
    }

    public void setNumberMax(String numberMax) {
        this.numberMax = numberMax;
    }

    public String getNumberMaxMin() {
        return numberMaxMin;
    }

    public void setNumberMaxMin(String numberMaxMin) {
        this.numberMaxMin = numberMaxMin;
    }

    public String getNumberFixed() {
        return numberFixed;
    }

    public void setNumberFixed(String numberFixed) {
        this.numberFixed = numberFixed;
    }

    public String getNumberProhibitEmpty() {
        return numberProhibitEmpty;
    }

    public void setNumberProhibitEmpty(String numberProhibitEmpty) {
        this.numberProhibitEmpty = numberProhibitEmpty;
    }

    public interface Test1{}
}
