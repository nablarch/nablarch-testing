package nablarch.test.core.util.generator;


import static nablarch.test.core.util.generator.JapaneseCharacterSet.ALPHABET;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ASCII_SYMBOL;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.HANKAKU_KANA_CHARS;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.NUMERIC;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_ALPHA_CHARS;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_ETC;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_HIRAGANA_CHARS;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_KANJI;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_KATAKANA_CHARS;
import static nablarch.test.core.util.generator.JapaneseCharacterSet.ZENKAKU_NUM_CHARS;

/**
 * 日本語文字種生成クラスの基本実装クラス。
 * 本クラスでは、文字種と文字集合の組み合わせを定義するのみで、
 * 実際の処理はスーパークラス（{@link CharacterGeneratorBase}）で実施している。
 * 以下の文字種を生成できる。
 * <ul>
 * <li>半角英字</li>
 * <li>半角数字</li>
 * <li>半角記号</li>
 * <li>半角カナ</li>
 * <li>全角英字</li>
 * <li>全角数字</li>
 * <li>全角ひらがな</li>
 * <li>全角カタカナ</li>
 * <li>全角漢字</li>
 * <li>全角記号その他</li>
 * <li>サロゲートペア</li>
 * <li>中国語（Unicode上に含まれるがJIS X0213に含まれない漢字）</li>
 * <li>外字</li>
 * </ul>
 *
 * @author T.Kawasaki
 */
public class BasicJapaneseCharacterGenerator extends CharacterGeneratorBase {

    /** 文字種と文字集合の組み合わせ */
    private static final String[][] TYPE_CHARS_PAIRS = {
            {"半角英字", ALPHABET},
            {"半角数字", NUMERIC},
            {"半角記号", ASCII_SYMBOL},
            {"半角カナ", HANKAKU_KANA_CHARS},
            {"全角英字", ZENKAKU_ALPHA_CHARS},
            {"全角数字", ZENKAKU_NUM_CHARS},
            {"全角ひらがな", ZENKAKU_HIRAGANA_CHARS},
            {"全角カタカナ", ZENKAKU_KATAKANA_CHARS},
            {"全角漢字", ZENKAKU_KANJI},
            {"全角記号その他", ZENKAKU_ETC},
            {"中国語", "\u4F60"},   //  你(ニーハオのニー「ｲ尓」 CJK統合漢字)
            {"サロゲートペア", "\uD867\uDE3D\uD842\uDFB7"},  // 𩸽 (ホッケ「魚花」U+29E3D CJK統合漢字拡張B)、𠮷 (つちよし U+20BB7 CJK統合漢字拡張B)
            {"改行", "\r\n"},
            {"外字", "㈱"} // CJK統合漢字と重複しないように
    };

    /** コンストラクタ */
    public BasicJapaneseCharacterGenerator() {
        super(TYPE_CHARS_PAIRS);
    }

}
