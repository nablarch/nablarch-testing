package nablarch.test;

import nablarch.fw.DataReader;
import nablarch.fw.DataReader.NoMoreRecord;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Result;
import nablarch.fw.reader.DatabaseTableQueueReader;

/**
 * データリーダで初回に取得したデータを処理するハンドラ実装クラス。
 * <p/>
 * 本ハンドラは、後続のハンドラから{@link NoMoreRecord}が返却されるまで
 * 後続のハンドラを繰り返し実行する。
 * <p/>
 * 常駐バッチ（サービス型）処理のテスト時に、{@link nablarch.fw.handler.RequestThreadLoopHandler}の代わりに
 * 本ハンドラを設定することで、テスト実行前にセットアップした要求データを処理後にバッチ処理を終了することができる。
 * <p/>
 * ※本ハンドラではなく{@link nablarch.fw.handler.RequestThreadLoopHandler}でテストを実行した場合、
 * 入力データを全て処理し終わった後も引き続き要求データの検索処理が継続される。
 * このため、バッチ処理が終了せずにテストが実施できなくなる問題が発生する。
 *
 * @author hisaaki sioiri
 */
public class OneShotLoopHandler implements Handler<Object, Object> {

    /**
     * {@inheritDoc}
     *
     * 後続のハンドラから{@link NoMoreRecord}が返却されるまで、
     * または{@link nablarch.fw.ExecutionContext#hasNextData()}がfalseを返すまで後続のハンドラを繰り返し実行する。
     */
    @Override
    public Object handle(Object data, ExecutionContext context) {

        DataReader<?> dataReader = context.getDataReader();

        if (dataReader instanceof DatabaseTableQueueReader) {
            // DatabaseTableQueueReaderの場合には、内部で保持しているオリジナルのリーダに差し替える。
            context.setDataReader(((DatabaseTableQueueReader) dataReader).getOriginalReader());
        }

        Result result = null;
        while (context.hasNextData()) {
            result = context.copy().handleNext(data);

            if (result instanceof NoMoreRecord) {
                // これ以上データが存在しない場合は処理を終了する
                break;
            }
        }
        return result;
    }
}

