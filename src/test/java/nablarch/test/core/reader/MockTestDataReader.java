package nablarch.test.core.reader;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * テスト用の{@link TestDataReader}実装クラス。
 *
 * @author T.Kawasaki
 */
class MockTestDataReader implements TestDataReader {
    /** 返却する文字列のイテレータ */
    private final Iterator<List<String>> iterator;

    /** コンストラクタ */
    MockTestDataReader() {
        this(Collections.<List<String>>emptyList());
    }

    /**
     * コンストラクタ
     * @param lines 返却する文字列
     */
    MockTestDataReader(List<List<String>> lines) {
        this.iterator = lines.iterator();
    }

    /** {@inheritDoc */
    public void open(String path, String dataName) {
    }

    /** {@inheritDoc */
    public void close() {
    }

    /** {@inheritDoc */
    public List<String> readLine() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return true;
    }

    @Override
    public boolean isDataExisting(String basePath, String resourceName) {
        return true;
    }
}
