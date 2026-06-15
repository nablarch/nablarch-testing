package nablarch.test.tool.converter;

/**
 * 変換処理で回復不能な状況（入力先が存在しない・上書き禁止下での出力衝突・読み書き失敗など）を表す非検査例外。
 *
 * <p>
 * ミッションクリティカルなテスト基盤として、変換は「黙って一部だけ成功」させず、異常は即座に表面化させる
 * 方針を採る。入口（{@link TestDataConverter}）は本例外を呼び出し側（テストコード・CLI）へ伝播する。
 * </p>
 *
 * @author kiyobot
 */
public class ConverterException extends RuntimeException {

    /**
     * メッセージを指定して例外を生成する。
     *
     * @param message 詳細メッセージ
     */
    public ConverterException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定して例外を生成する。
     *
     * @param message 詳細メッセージ
     * @param cause   原因
     */
    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
