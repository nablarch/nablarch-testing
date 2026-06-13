package nablarch.test.core.reader;


import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 固定長ファイルのテストデータを解析するクラス。
 *
 * @param <T> 解析結果の型
 * @author T.Kawasaki
 */
public abstract class DataFileParser<T extends DataFile> extends GroupDataParsingTemplate<List<T>> {

    /** 解析結果の固定長ファイル */
    private List<T> result = new ArrayList<T>();

    /** 現在処理中のファイル */
    protected T currentFile;                      // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** 現在処理中の断片 */
    protected DataFileFragment currentFragment;   // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** 現在の処理状態 */
    protected Status status = Status.NONE;        // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** データタイプ */
    private DataType targetType;

    /** 処理状態 */
    protected static enum Status {
        /** 未処理 */
        NONE,
        /** ディレクティブ、フィールド名称読み取り中 */
        READING_DIRECTIVES_AND_NAMES,
        /** フィールドデータ型読み取り中 */
        READING_TYPES,
        /** フィールド長読み取り中 */
        READING_LENGTHS,
        /** フィールドデータ行読み取り中 */
        READING_VALUES
    }

    /**
     * コンストラクタ
     *
     * @param reader       リーダ
     * @param interpreters 解釈クラス
     * @param targetType   解析対象のデータタイプ
     */
    public DataFileParser(TestDataReader reader, List<TestDataInterpreter> interpreters, DataType targetType) {
        super(reader, interpreters, targetType);
        this.targetType = targetType;
    }


    /** {@inheritDoc} */
    @Override
    final void onReadLine(List<String> original) {

        List<String> line = NablarchTestUtils.trimTailCopy(original); // キャッシュを破壊しないようにコピーして編集
        switch (status) {
            case READING_DIRECTIVES_AND_NAMES:  //------------- ディレクティブ、フィールド名称
                onReadingDirectives(line);
                break;
            case READING_TYPES:    //---------------- 型
                onReadingTypes(line);
                break;
            case READING_LENGTHS:  //---------------- フィールド長
                onReadingLengths(line);
                break;
            case READING_VALUES:   //---------------- データ行
                onReadingValues(line);
                break;
            default:
                // ここに到達することはありえない。
                throw new IllegalStateException(
                        "invalid status[" + status + "] line=[" + original + "]");
        }
    }

    /** キャッシュ(同一データを複数回読み込まないようにするため) */
    private static Map<String, List<? extends DataFile>> cache = NablarchTestUtils.createLRUMap(8);

    @Override
    void parse(String id) {
        String key = directory + '/' + resource + '/' + targetType + '/' + id;
        if (cache.containsKey(key)) {
            @SuppressWarnings("unchecked")
            List<T> temp = (List<T>) cache.get(key);
            // キャッシュを使いたいが、取得した先で内容を書き換えられてしまうのでキャッシュの値は使わず再度オブジェクトを構築する。
            // ただし、データが存在しない場合には、不必要なデータの検索処理が行われないため、パフォーマンスが向上する。
            if (temp.isEmpty()) {
                result = temp;
            } else {
                super.parse(id);
            }
        } else {
            cache.put(key, result);
            super.parse(id);
        }
    }

    /** {@inheritDoc} */
    @Override
    void onTargetTypeFound(List<String> line) {
        // ファイル名
        String filePath = getTypeValue(line);
        currentFile = createNewFile(filePath);
        result.add(currentFile);
        status = Status.READING_DIRECTIVES_AND_NAMES;
    }

    /** {@inheritDoc} */
    @Override
    List<T> getResult() {
        return result;
    }

    /**
     * 新しいファイルを生成する。
     *
     * @param filePath ファイルパス
     * @return ファイル
     */
    protected abstract T createNewFile(String filePath);

    /**
     * ディレクティブ行を読み込む。
     *
     * @param line 行データ
     */
    protected void onReadingDirectives(List<String> line) {
        boolean isDirective = processDirectives(line);
        if (!isDirective) {
            onReadingNames(line);
        }
    }

    /**
     * フィールド名の行を読み込む。
     *
     * @param line 行データ
     */
    protected void onReadingNames(List<String> line) {
        createNewFragment(line);
        status = Status.READING_TYPES;
    }

    /**
     * データ型行を読み込む。
     *
     * @param line 行データ
     */
    protected void onReadingTypes(List<String> line) {
        currentFragment.setTypes(tail(line));
        status = Status.READING_LENGTHS;
    }

    /**
     * フィールド長の行を読み込む。
     *
     * @param line 行データ
     */
    protected void onReadingLengths(List<String> line) {
        currentFragment.setLengths(tail(line));
        status = Status.READING_VALUES;
    }

    /**
     * データ行を読み込む。
     *
     * @param line 行データ
     */
    protected void onReadingValues(List<String> line) {

        if (isDataRow(line)) {
            // データ行の場合
            currentFragment.addValue(tail(line));
        } else {
            createNewFragment(line); //------- 名前（新しい）
            status = Status.READING_TYPES;
        }
    }

    /**
     * データ行であるかどうか判定する。
     * 以下の条件のいずれかに合致する場合、データ行とみなす。
     * <ul>
     * <li>行データが空（要素数０)の場合、すなわち空行の場合</li>
     * <li>行データの先頭が空文字</li>
     * </ul>
     *
     * @param line 行データ
     * @return データ行の場合、真
     */
    private boolean isDataRow(List<String> line) {
        if (line.isEmpty()) {
            return true;
        }
        String firstColumn = line.get(0);
        return StringUtil.isNullOrEmpty(firstColumn);
    }

    /**
     * ディレクティブ行を処理する。<br/>
     * 与えられた行データがディレクティブ行の場合、ディレクティブを取得する。
     *
     * @param line 行データ
     * @return 与えられた行がディレクティブ行の場合、真
     */
    protected boolean processDirectives(List<String> line) {
        if (line.size() < 2) {
            // ２列はあるはず。
            throw new IllegalStateException(
                    "directive or data names row must have two columns at least. " + line);
        }
        String directive = line.get(0);
        String value = line.get(1);
        if (isDirective(directive)) {
            currentFile.setDirective(directive, value);
            return true;
        }
        return false; // 処理した行はディレクティブでない。
    }

    /**
     * ディレクティブかどうか判定する。<br/>
     *
     * @param key キー
     * @return 与えられたキーがディレクティブであれば真
     */
    protected abstract boolean isDirective(String key);


    /**
     * 新しい断片を生成する。
     *
     * @param fieldNamesLine フィールド名称行
     */
    private void createNewFragment(List<String> fieldNamesLine) {
        currentFragment = currentFile.getNewFragment();
        currentFragment.setRecordType(fieldNamesLine.get(0));
        currentFragment.setNames(tail(fieldNamesLine));
    }

    /**
     * 先頭要素を削除したリストを返却する。
     * 対象となるリストが空の場合（要素数0）、空のリストをそのまま返却する。
     *
     * @param <TYPE> リスト要素の型
     * @param list   対象となるリスト
     * @return 先頭要素を除いたリスト
     */
    protected <TYPE> List<TYPE> tail(List<TYPE> list) {
        if (list.isEmpty()) {
            return list;
        }
        return list.subList(1, list.size());
    }
}
