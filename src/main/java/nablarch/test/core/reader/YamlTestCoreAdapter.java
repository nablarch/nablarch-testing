package nablarch.test.core.reader;

import java.util.List;
import java.util.Map;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.reader.yaml.InterpreterResolver;
import nablarch.test.core.reader.yaml.YamlFileBuilder;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.core.reader.yaml.YamlMessageBuilder;
import nablarch.test.core.reader.yaml.YamlMessageBuilder.MessageContent;
import nablarch.test.core.reader.yaml.YamlSection;
import nablarch.test.core.reader.yaml.YamlTableDataBuilder;

/**
 * テストデータ変換ツール（{@code nablarch.test.tool.converter}）が、本体の YAML 読み込みを
 * 再利用して生の器を取り出すための薄いアダプタ。Excel 経路の {@link TestCoreReaderAdapter} と対称。
 *
 * <p>
 * 本体の YAML 読み込みは {@code reader.yaml} パッケージのビルダ
 * （{@link YamlTableDataBuilder}／{@link YamlFileBuilder}／{@link YamlMessageBuilder}）が
 * {@link YamlLoader#load} の返す順序保持 Map を走査して器を組み立てる。本アダプタはそれらビルダを
 * <b>{@link InterpreterResolver#raw() 値加工なし（空インタープリタ）}・デフォルト値補完なし</b>で配線し、
 * {@code ${...}}・{@code ${binaryFile:...}}・{@code null}・{@code ""} を記法のまま（未加工）保った
 * 生の本体器（{@link TableData}／{@link DataFile}／{@link MessagePool} 本文）を返す。
 * </p>
 * <p>
 * あわせて {@link #loadRawMap}（{@link YamlLoader#load} の透過）を備える。Excel が器の正規化を生行から
 * 復元するのに対し、YAML は器が正規化する値（カラム名の大文字化・長さ省略 {@code -}・型表記）の原文を
 * この順序保持 Map から復元する（設計書 §共通「器が正規化する値の原文復元」＝Excel=生行／YAML=YamlLoader Map）。
 * </p>
 *
 * @author kiyobot
 */
public class YamlTestCoreAdapter {

    /** テーブル系ビルダ（空インタープリタ・補完なしで配線） */
    private final YamlTableDataBuilder tableBuilder;

    /** ファイル系ビルダ（空インタープリタで配線） */
    private final YamlFileBuilder fileBuilder;

    /** メッセージ系ビルダ（空インタープリタで配線） */
    private final YamlMessageBuilder messageBuilder;

    /**
     * コンストラクタ。ビルダを値加工なし（{@link InterpreterResolver#raw()}）で配線する。
     * <p>
     * テーブル構築に必要な {@link nablarch.test.core.db.DbInfo} はスタブ（{@link StubDbInfo}）を用いる。
     * 読み込み経路で実際に参照されるのはカラム型（一律 VARCHAR）のみで、値は型に依存せず生のまま格納される。
     * </p>
     */
    public YamlTestCoreAdapter() {
        InterpreterResolver raw = InterpreterResolver.raw();
        DefaultValues defaultValues = new BasicDefaultValues();
        this.tableBuilder = new YamlTableDataBuilder(new StubDbInfo(), defaultValues, raw);
        this.fileBuilder = new YamlFileBuilder(raw);
        this.messageBuilder = new YamlMessageBuilder(raw);
    }

    /**
     * YAML ファイルのトップレベル Map（原文。順序保持）をそのまま返す。
     * <p>
     * 変換ツールは器（構造）と本 Map（原文）を突き合わせて中間モデルを組み立てる。
     * </p>
     *
     * @param path     取得元パス
     * @param resource リソース名（拡張子なし）
     * @return YAML トップレベル Map（空ファイルの場合は空 Map）
     */
    public Map<String, Object> loadRawMap(String path, String resource) {
        return YamlLoader.load(path, resource);
    }

    /**
     * YAML ファイルが存在するかどうかを返す。
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @return 存在する場合 true
     */
    public boolean isResourceExisting(String path, String resource) {
        return YamlLoader.isResourceExisting(path, resource);
    }

    /**
     * テーブルデータを取り出す。
     * <p>
     * デフォルト値補完（{@code fillDefaultValues}）は行わず、指定データタイプ・グループの生の
     * {@link TableData} 一覧を返す。
     * </p>
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @param groupId  整形済みグループ ID（例: {@code "[case01]"} または {@code ""}）
     * @param type     データタイプ（{@link DataType#SETUP_TABLE_DATA}／
     *                 {@link DataType#EXPECTED_TABLE_DATA}／{@link DataType#EXPECTED_COMPLETED}）
     * @return テーブルデータ一覧
     * @throws IllegalArgumentException データタイプがテーブル系でない場合
     */
    public List<TableData> readTables(String path, String resource, String groupId, DataType type) {
        Map<String, Object> yaml = loadRawMap(path, resource);
        return tableBuilder.buildTableDataList(yaml, tableSectionKey(type), groupId, false, path);
    }

    /**
     * {@code List<Map<String, String>>}形式（{@code list_maps}）のデータを取り出す。
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @param id       list_maps エントリの id
     * @return 行データ一覧（見つからない場合は空）
     */
    public List<Map<String, String>> readListMap(String path, String resource, String id) {
        Map<String, Object> yaml = loadRawMap(path, resource);
        return tableBuilder.buildListMapRows(yaml, id, path);
    }

    /**
     * ファイル（固定長／可変長）を取り出す。固定長・可変長の区別はエントリの {@code type} で決まる。
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @param groupId  整形済みグループ ID（例: {@code "[case01]"} または {@code ""}）
     * @param type     データタイプ（{@link DataType#SETUP_FIXED}／{@link DataType#EXPECTED_FIXED}／
     *                 {@link DataType#SETUP_VARIABLE}／{@link DataType#EXPECTED_VARIABLE}）
     * @return ファイル一覧
     * @throws IllegalArgumentException データタイプがファイル系でない場合
     */
    public List<DataFile> readFiles(String path, String resource, String groupId, DataType type) {
        Map<String, Object> yaml = loadRawMap(path, resource);
        return fileBuilder.buildDataFileList(yaml, fileSectionKey(type), groupId, path);
    }

    /**
     * メッセージ（{@link DataType#MESSAGE}）を取り出す。本文（固定長ファイルの器）と FW 制御ヘッダを併せ持つ。
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @param id       メッセージ ID
     * @return 本文と FW 制御ヘッダ。対象が存在しない場合は {@code null}
     */
    public MessageContent readMessage(String path, String resource, String id) {
        Map<String, Object> yaml = loadRawMap(path, resource);
        return messageBuilder.buildMessageContent(yaml, YamlSection.KEY_MESSAGES, id, true, path);
    }

    /**
     * 送信同期メッセージ（要求/応答電文 4 種）のうち、指定グループに属する全ブロックの本文
     * （固定長ファイルの器）を取り出す。FW 制御ヘッダは送信系では常に空のため返さない。
     *
     * @param path     取得元パス
     * @param resource リソース名
     * @param groupId  グループ ID（{@code group_id} と生値で一致比較する）
     * @param type     データタイプ（送信系 4 種のいずれか）
     * @return 本文（固定長ファイルの器）一覧（記述順。対象が無ければ空）
     * @throws IllegalArgumentException データタイプが送信系でない場合
     */
    public List<FixedLengthFile> readSendSyncMessages(String path, String resource, String groupId, DataType type) {
        Map<String, Object> yaml = loadRawMap(path, resource);
        return messageBuilder.buildSendSyncBodies(yaml, sendSyncSectionKey(type), groupId, path);
    }

    /**
     * テーブル系データタイプを YAML セクションキーへ変換する。
     *
     * @param type データタイプ
     * @return セクションキー
     * @throws IllegalArgumentException テーブル系でない場合
     */
    private static String tableSectionKey(DataType type) {
        switch (type) {
            case SETUP_TABLE_DATA:  return YamlSection.KEY_SETUP_TABLES;
            case EXPECTED_TABLE_DATA: return YamlSection.KEY_EXPECTED_TABLES;
            case EXPECTED_COMPLETED:  return YamlSection.KEY_EXPECTED_COMPLETE_TABLES;
            default:
                throw new IllegalArgumentException(
                        "unsupported data type for readTables. type=[" + type + "]");
        }
    }

    /**
     * ファイル系データタイプを YAML セクションキーへ変換する。固定長／可変長はセクションを分けない。
     *
     * @param type データタイプ
     * @return セクションキー
     * @throws IllegalArgumentException ファイル系でない場合
     */
    private static String fileSectionKey(DataType type) {
        switch (type) {
            case SETUP_FIXED:
            case SETUP_VARIABLE:
                return YamlSection.KEY_SETUP_FILES;
            case EXPECTED_FIXED:
            case EXPECTED_VARIABLE:
                return YamlSection.KEY_EXPECTED_FILES;
            default:
                throw new IllegalArgumentException(
                        "unsupported data type for readFiles. type=[" + type + "]");
        }
    }

    /**
     * 送信系データタイプを YAML セクションキーへ変換する。
     *
     * @param type データタイプ
     * @return セクションキー
     * @throws IllegalArgumentException 送信系 4 種でない場合
     */
    private static String sendSyncSectionKey(DataType type) {
        switch (type) {
            case EXPECTED_REQUEST_HEADER_MESSAGES:
            case EXPECTED_REQUEST_BODY_MESSAGES:
            case RESPONSE_HEADER_MESSAGES:
            case RESPONSE_BODY_MESSAGES:
                return YamlSection.dataTypeToSectionKey(type);
            default:
                throw new IllegalArgumentException(
                        "unsupported data type for readSendSyncMessages. type=[" + type + "]");
        }
    }
}
