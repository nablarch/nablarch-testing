package nablarch.test.core.util.generator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * {@link CharacterGeneratorBase.RandomStringGenerator}のテストクラス
 *
 * @author T.Kawasaki
 */
public class RandomStringGeneratorTest {

    /** 引数がnullの場合例外が発生すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNull() {
        new CharacterGeneratorBase.RandomStringGenerator(null);
    }

    /** 引数が空文字のケース */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmpty() {
        new CharacterGeneratorBase.RandomStringGenerator("");
    }

    /** ランダムに文字が生成できること */
    @Test
    public void testGenerateCharacter() {

        CharacterGeneratorBase.RandomStringGenerator target
                = new CharacterGeneratorBase.RandomStringGenerator("abc");
        for (int i = 0; i < 10; i++) {  // 念のため10回ほど
            // abcという文字列を元に、ランダムな文字列が生成されるはず
            String actual = String.valueOf(target.generate());
            assertTrue(
                    "expected [abc]? but was " + actual,
                    actual.matches("[abc]?"));
        }
    }

    /** ランダムな文字列が生成できること */
    @Test
    public void testGenerateString() {
        CharacterGeneratorBase.RandomStringGenerator target
                = new CharacterGeneratorBase.RandomStringGenerator("abcdefghijklmn");
        String actual = target.generate(100);
        assertTrue(
                "expected [a-n]{100}? but was " + actual,
                actual.matches("[a-n]{100}"));
    }

    /**
     * ランダムな文字列が生成できること。
     * 文字集合にサロゲートペアが含まれていても、文字数カウントがずれないこと
     */
    @Test
    public void testGenerateSurrogatePairString() {
        CharacterGeneratorBase.RandomStringGenerator target
                = new CharacterGeneratorBase.RandomStringGenerator("\uD867\uDE3D\uD842\uDFB7");
        String actual = target.generate(100);
        assertThat(Character.codePointCount(actual, 0, actual.length()), is(100));
    }

    /** 桁数0の文字列を生成した場合、空文字が返却されるケース */
    @Test
    public void testGenerateWithZero() {
        CharacterGeneratorBase.RandomStringGenerator target
                = new CharacterGeneratorBase.RandomStringGenerator("abcdefghijklmn");
        assertEquals("", target.generate(0));
    }

    /** 桁数が負数の場合、例外が発生すること */
    @Test(expected = IllegalArgumentException.class)
    public void testGenerateWithNegativeNumber() {
        CharacterGeneratorBase.RandomStringGenerator target
                = new CharacterGeneratorBase.RandomStringGenerator("abcdefghijklmn");
        target.generate(-1);
    }


}
