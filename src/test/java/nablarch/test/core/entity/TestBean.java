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
            @Length(max = 10, groups = Test1.class, message = "{MSG00024}")
    })
    @SystemChar(charsetDef = "半角数字")
    private String numberMax;

    @Length.List({
            @Length(max = 50, min = 20),
            @Length(max = 35, min = 15, groups = Test1.class, message = "{MSG00011}")

    })
    @SystemChar(charsetDef = "半角数字")
    private String numberMaxMin;

    @Length.List({
            @Length(min = 50),
            @Length(min = 10, groups = Test1.class, message = "{MSG00025}")
    })
    @SystemChar(charsetDef = "半角数字")
    private String numberMin;

    @Length.List({
            @Length(max = 10, min = 10),
            @Length(max = 20, min = 20, groups = Test1.class, message = "{MSG00023}")
    })
    @SystemChar(charsetDef = "半角数字")
    private String numberFixed;

    @Required.List({
            @Required,
            @Required(groups = Test1.class, message = "{MSG00010}")
    })
    @Length.List({
            @Length(max = 50),
            @Length(max = 10, groups = Test1.class, message = "{MSG00024}")
    })
    @SystemChar(charsetDef = "半角数字")
    private String numberProhibitEmpty;

    @SystemChar.List({
            @SystemChar(charsetDef = "ASCII文字", message = "{MSG00012}"),
            @SystemChar(charsetDef = "半角英字", groups = Test1.class, message = "{MSG90001}")
    })
    private String ascii;

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

    public String getNumberMin() {
        return numberMin;
    }

    public void setNumberMin(String numberMin) {
        this.numberMin = numberMin;
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

    public String getAscii() {
        return ascii;
    }

    public void setAscii(String ascii) {
        this.ascii = ascii;
    }

    public interface Test1{}
}
