package nablarch.test.core.entity;

import nablarch.core.validation.validator.AlnumChar;
import nablarch.core.validation.validator.AlphaChar;
import nablarch.core.validation.validator.AsciiChar;
import nablarch.core.validation.validator.Length;
import nablarch.core.validation.validator.NumberChar;
import nablarch.core.validation.validator.NumberRange;
import nablarch.core.validation.validator.Required;
import nablarch.core.validation.validator.japanese.HankakuChar;
import nablarch.core.validation.validator.japanese.HankakuKanaChar;
import nablarch.core.validation.validator.japanese.ZenkakuAndGaijiChar;
import nablarch.core.validation.validator.japanese.ZenkakuChar;
import nablarch.core.validation.validator.japanese.ZenkakuHiraganaChar;
import nablarch.core.validation.validator.japanese.ZenkakuKatakanaChar;
import nablarch.core.validation.validator.unicode.SystemChar;

/**
 * 単項目バリデーションのテスト用エンティティクラス
 * @author T.Kawasaki
 */
public class TestEntity {

    @Length(max = 50)
    @AsciiChar
    public void setAscii(String s) {
    }

    @Length(max = 50)
    @HankakuKanaChar
    public void setHankakuKana(String hankakuKana) {
    }

    @Length(min = 1, max = 100)
    @HankakuChar
    public void setHankaku(String hankaku) {
    }

    @AlphaChar
    @Length(max = 5, min = 5)
    public void setAlpha(String s) {
    }

    @Required
    @Length(max = 1, min = 1)
    @AlnumChar
    public void setAlphaNumeric(String s) {
    }

    @Length(max = 50)
    @ZenkakuChar
    public void setZenkaku(String s) {
    }

    @Length(max = 4)
    @NumberChar
    public void setNumber(String s) {
    }


    @Length(max = 30, min = 5)
    @ZenkakuHiraganaChar
    public void setZenkakuHiragana(String s) {
    }

    @Required
    @Length(max = 10, min = 5)
    @ZenkakuKatakanaChar
    public void setZenkakuKatakana(String s) {
    }

    @Required
    @Length(max = 35)
    @ZenkakuAndGaijiChar
    public void setZenkakuAndGaiji(String s) {
    }

    @Required
    @Length(max = 3)
    @NumberRange(max = 100, min = 10)
    public void setNumberRange(int i) {
    }

    @Required
    @Length(max = 10)
    @SystemChar(messageId = "MSG90001")
    public void setSystemChar(String s) {
    }

    @Required
    @Length(max = 100)
    @SystemChar(messageId = "MSG90001", allowLineSeparator = true)
    public void setSystemCharWithLS(String s) {
    }

    @Length(max = 50, messageId = "MSG20013")
    @AsciiChar
    public void setAsciiWithMaxMessage(String s) {
    }

    @Length(max = 50, min = 10, messageId = "MSG20011")
    @AsciiChar
    public void setAsciiWithMaxMinMessage(String s) {
    }

    @Length(max = 20, min = 20, messageId = "MSG20012")
    @AsciiChar
    public void setAsciiWithFixMessage(String s) {
    }

    @Required(messageId = "MSG20010")
    @Length(max = 20, messageId = "MSG20013")
    @AsciiChar
    public void setAsciiWithEmptyMessage(String s) {
    }
}
