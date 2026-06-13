package nablarch.test.core.reader;

import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * メッセージ（同期送信）を解析するクラス。
 * @author Masato Inoue
 */
public class GroupMessageParser extends GroupDataParsingTemplate<List<RequestTestingMessagePool>> {

    /** 処理を委譲するパーサ */
    private final MessageParser delegate;

    /** {@inheritDoc} */
    @Override
    void onReadLine(List<String> line) {
        delegate.onReadLine(line);
        
    }

    /** {@inheritDoc} */
    @Override
    void onTargetTypeFound(List<String> line) {
        delegate.onTargetTypeFound(line);
    }
    
    
    /**
     * コンストラクタ。
     * @param reader       リーダ
     * @param interpreters 解釈クラス
     * @param targetType   処理対象のデータ型
     */
    public GroupMessageParser(TestDataReader reader, List<TestDataInterpreter> interpreters, DataType targetType) {
        super(reader, interpreters, targetType);
        delegate = new SendSyncMessageParser(reader, interpreters, targetType);
    }

    
    
    /** {@inheritDoc} */
    @Override
    List<RequestTestingMessagePool> getResult() {
        FixedLengthFileParser delegateParser = delegate.getDelegate();
        List<FixedLengthFile> dataList = delegateParser.getResult();
        if (dataList.isEmpty()) {
            return null;
        }
        ArrayList<RequestTestingMessagePool> pools = new ArrayList<RequestTestingMessagePool>();
        for (FixedLengthFile data : dataList) {
            Map<String, String> emptyHeader = Collections.emptyMap(); // FWヘッダ取得機能は使用しないので、何も設定しない
            RequestTestingMessagePool messagePoolEx = new RequestTestingMessagePool(data, emptyHeader);
            messagePoolEx.setRequestId(data.getPath());
            pools.add(messagePoolEx);
            
        }
        return pools;
    }

}
