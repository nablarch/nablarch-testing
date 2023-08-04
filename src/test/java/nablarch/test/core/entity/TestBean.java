package nablarch.test.core.entity;

import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.Required;
import nablarch.core.validation.ee.SystemChar;

/**
 * 単項目バリデーションのテスト用エンティティクラス
 * @author T.Kawasaki
 */
public class TestBean {

    @Length(max = 50)
    @SystemChar(charsetDef = "半角数字")
    public String numberMax;

    @Length(max = 50, min = 20)
    @SystemChar(charsetDef = "半角数字")
    public String numberMaxMin;

    @Length(max = 10, min = 10)
    @SystemChar(charsetDef = "半角数字")
    public String numberFixed;

    @Required
    @Length(max = 5, min = 5)
    @SystemChar(charsetDef = "半角数字")
    public String numberProhibitEmpty;
}
