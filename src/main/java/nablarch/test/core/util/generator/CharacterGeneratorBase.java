package nablarch.test.core.util.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

import nablarch.core.util.annotation.Published;

/**
 * 文字列生成基底クラス。
 * 単純に、文字種と文字集合をカスタマイズしたい場合は本クラスを継承し、
 * コンストラクタで文字種と文字集合のペアを本クラスに渡せばよい。
 * （実装例は、{@link BasicJapaneseCharacterGenerator}を参照）
 *
 * @author T.Kawasaki
 */
@Published(tag = "architect")
public abstract class CharacterGeneratorBase implements CharacterGenerator {

    /**
     * 生成クラス格納用Map
     * キー：文字種の名前
     * 値：文字列生成クラス
     */
    private final Map<String, RandomStringGenerator> generators;

    /**
     * コンストラクタ
     * 引数には、文字種の名前と文字集合のペアを渡す。
     * (例： {"numeric", "01234567689"})
     *
     * @param typeCharsPairs 文字種の名前と文字集合のペア
     */
    protected CharacterGeneratorBase(String[][] typeCharsPairs) {
        generators = new HashMap<String, RandomStringGenerator>(typeCharsPairs.length);
        for (String[] e : typeCharsPairs) {
            String type = e[0];
            String charset = e[1];
            generators.put(type, new RandomStringGenerator(charset));
        }
    }

    /**
     * 与えられた文字種の文字列を生成する。
     * 不明な文字種が与えられた場合は例外が発生する。
     *
     * @param charsetName 文字種の名前
     * @param length      生成する文字列の長さ
     * @return 文字列
     */
    public final String generate(String charsetName, int length) {
        RandomStringGenerator generator = generators.get(charsetName);
        if (generator == null) {
            throw new IllegalArgumentException("unknown charsetName. charsetName=[" + charsetName + "]");
        }
        return generator.generate(length);
    }

    /**
     * Stringを元にした文字列生成クラス。
     * 元になる文字集合からランダムに文字を選択して文字列を生成する。
     *
     * @author T.Kawasaki
     */
    protected static class RandomStringGenerator {

        /** 乱数 */
        private final Random random = new Random();

        /** 文字集合 */
        private final String charset;

        /**
         * コンストラクタ
         *
         * @param sourceCharset 生成元となる文字集合
         */
        protected RandomStringGenerator(String sourceCharset) {
            if (isNullOrEmpty(sourceCharset)) {
                throw new IllegalArgumentException("sourceCharset must not be null or empty.");
            }
            this.charset = sourceCharset;
        }

        /**
         * 文字列を生成する
         *
         * @param length 文字列長
         * @return 文字列
         */
        String generate(int length) {
            if (length < 0) {
                throw new IllegalArgumentException("argument must not be negative.");
            }
            StringBuilder result = new StringBuilder(length);
            while (length > result.codePointCount(0, result.length())) {
                result.append(generate());
            }
            return result.toString();
        }

        /**
         * 文字を生成する。
         *
         * @return 文字
         */
        char generate() {
            int index = random.nextInt(charset.length());
            return charset.charAt(index);
        }
    }
}
