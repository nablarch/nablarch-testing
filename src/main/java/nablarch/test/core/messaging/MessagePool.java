package nablarch.test.core.messaging;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FixedLengthDataRecordFormatter;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.test.Assertion;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.file.FixedLengthFile;

/**
 * テストショット毎のメッセージを保持するクラス。
 *
 * @author T.Kawasaki
 */
@Published(tag = "architect")
public class MessagePool {
    /**
     * フォーマッタ。<br/>
     * テストデータから作成したレイアウトを使用する。
     */
    private final DataRecordFormatter formatter = new FixedLengthDataRecordFormatter();

    /** フレームワーク制御ヘッダ */
    private final Map<String, String> fwHeader;

    /** メッセージのイテレータ */
    private final Iterator<DataRecord> iterator;

    /** メッセージのリスト */
    private final List<DataRecord> records;

    /** 元のデータ */
    private final FixedLengthFile source;

    /** デフォルトのレイアウト定義 */
    private final LayoutDefinition defaultLayout;
    
    /** SystemRepositoryに設定するデータレコードとしてアサートを行うファイルタイプを管理するためのキー */
    private static final String ASSERT_AS_MAP_KEY = "messaging.assertAsMapFileType";
    
    /**
     * コンストラクタ。
     *
     * @param source   元のデータ
     * @param fwHeader フレームワーク制御ヘッダ
     */
    public MessagePool(FixedLengthFile source, Map<String, String> fwHeader) {
        this.source = source;
        this.fwHeader = fwHeader;
        this.defaultLayout = source.createLayout();
        this.formatter.setDefinition(this.defaultLayout);
        this.records = toDataRecords();
        this.iterator = toDataRecords().iterator();
    }

    /**
     * テストデータ（メッセージ）投入の準備をする。
     *
     * @return テストデータ（メッセージ）投入用クラスのインスタンス
     */
    Putter prepareForPut() {
        return new Putter();
    }

    /**
     * 結果検証の準備をする。
     *
     * @return 検証用クラスのインスタンス
     */
    Comparator prepareForCompare() {
        return new Comparator();
    }

    /**
     * データレコードに変換する。
     *
     * @return データレコード
     */
    List<DataRecord> toDataRecords() {
        return source.toDataRecords();
    }

    /**
     * フレームワーク制御ヘッダを取得する。
     *
     * @return フレームワーク制御ヘッダ
     */
    Map<String, String> getFwHeader() {
        return fwHeader;
    }

    /** テストデータ投入用クラス。<br/> */
    final class Putter {

        /**
         * 送信メッセージにメッセージ本文を設定する。
         *
         * @param headerFormatter FWヘッダのフォーマッタ
         * @return 送信メッセージ
         * @throws NoSuchElementException メッセージがない場合
         */
        SendingMessage createSendingMessage(DataRecordFormatter headerFormatter) throws NoSuchElementException {
            SendingMessage msg = new SendingMessage();
            // テストデータ取得
            DataRecord currentData = iterator.next();
            // テストデータ変換
            currentData = convertByFileType(currentData);
            // 対応するレイアウト定義を生成
            LayoutDefinition ld = createLayoutFromDataRecord(currentData);
            
            if (headerFormatter != null) {
                //FWヘッダのフォーマッタがある場合は、FWヘッダを1レコード目に追加する必要があるとみなし、処理する。
                msg.setFormatter(headerFormatter).addRecord(fwHeader);
            }
            return msg.setFormatter(formatter.setDefinition(ld)).addRecord(currentData);
        }
    }

    /**
     * 結果検証用クラス。<br/>
     * 応答電文送信キューに期待するメッセージが格納されていることを検証する。
     */
    final class Comparator {

        /**
         * キューからメッセージを取得し、期待値との比較を行う。
         *
         * @param msgOnFail       比較失敗時のメッセージ
         * @param responseMessage 応答メッセージ
         */
        void compareBody(String msgOnFail, ReceivedMessage responseMessage) {
            // テストデータ取得
            DataRecord currentData = iterator.next();
            // テストデータ変換
            currentData = convertByFileType(currentData);
            // 対応するレイアウト定義を生成
            LayoutDefinition ld = createLayoutFromDataRecord(currentData);
            
            // データレコードとしてアサートを行うファイルタイプ
            Set<String> assertAsDataRecordFileTypes 
                    = isNullOrEmpty(SystemRepository.getString(ASSERT_AS_MAP_KEY))
                    ? NablarchTestUtils.asSet("Fixed")
                    : NablarchTestUtils.asSet(NablarchTestUtils.makeArray(SystemRepository.getString(ASSERT_AS_MAP_KEY)));

            String expectedFileType = getFileTypeFromDirective(ld);
            
            if (assertAsDataRecordFileTypes.contains(expectedFileType)) {
                // データレコードとして項目ごとにアサート
                
                List<DataRecord> actual = responseMessage.setFormatter(formatter.setDefinition(ld)).readRecords();
                List<DataRecord> expected = new ArrayList<DataRecord>();
                expected.add(currentData);
                Assertion.assertEquals(msgOnFail, expected, actual);
                
            } else {
                // 文字列として電文全体をアサート
                Charset expectedCharset = getCharsetFromDirective(ld);
                                            
                byte[] expectedBodyBytes = new SendingMessage()
                                            .setFormatter(formatter.setDefinition(ld))
                                            .addRecord(currentData)
                                            .getBodyBytes();
                
                String expectedBody = new String(expectedBodyBytes, expectedCharset);
                String actualBody = new String(responseMessage.getBodyBytes(), expectedCharset);
                
                Assertion.assertEquals(msgOnFail, expectedBody, actualBody);
            }
        }
    }

    /**
     * レイアウト定義のディレクティブからファイルタイプを取得。
     * @param ld レイアウト定義
     * @return レイアウト定義に指定されたファイルタイプ
     */
    private String getFileTypeFromDirective(LayoutDefinition ld) {
        return (String) ld.getDirective().get("file-type");
    }
    
    /**
     * レイアウト定義のディレクティブからエンコーディングを取得。
     * @param ld レイアウト定義
     * @return レイアウト定義に指定されたエンコーディング
     */
    private Charset getCharsetFromDirective(LayoutDefinition ld) {
        String expectedTextEncoding = (String) ld.getDirective().get("text-encoding");
        return Charset.forName(expectedTextEncoding);
    }
    
    /**
     * メッセージのイテレータを取得。
     * @return メッセージのイテレータ
     */
    protected Iterator<DataRecord> getIterator() {
        return iterator;
    }

    /**
     * FixedLengthFileを取得。
     * @return FixedLengthFile
     */
    protected FixedLengthFile getSource() {
        return source;
    }

    /**
     * DataRecordFormatterを取得。
     * @return DataRecordFormatter
     */
    public DataRecordFormatter getFormatter() {
        return formatter;
    }

    /**
     * メッセージのリストを取得。
     * @return メッセージのリスト
     */
    protected List<DataRecord> getRecords() {
        return records;
    }
    
    /**
     * データレコードのファイル種別に応じて変換します
     * @param dataRecord 対象データレコード
     * @return 変換後のデータレコード
     */
    protected DataRecord convertByFileType(DataRecord dataRecord) {
        return source.convertData(defaultLayout, dataRecord);
    }
    
    /**
     * デフォルトのレイアウトと、データレコードの内容を元にレイアウト定義を作成します
     * @param dataRecord データレコード
     * @return 新しいレイアウト定義
     */
    protected LayoutDefinition createLayoutFromDataRecord(DataRecord dataRecord) {
        return source.createDefinition(defaultLayout, dataRecord);
    }
}
