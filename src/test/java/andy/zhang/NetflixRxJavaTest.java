package andy.zhang;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by ZhangGuohua on 2017/3/29.
 */
public class NetflixRxJavaTest {
    private static final List<String> WORDS =
            Arrays.asList(
                    "the",
                    "quick",
                    "brown",
                    "fox",
                    "jumped",
                    "over",
                    "the",
                    "lazy",
                    "dog"
            );

    @Test
    public void testInSameThread() {
        // given:
        List<String> results = new ArrayList<>();
        Observable<String> observable = Observable.fromIterable(WORDS).zipWith(Observable.range(1, Integer.MAX_VALUE), (string, index) -> String.format("%d.%s", index, string));
        // when:
        observable.subscribe(results::add);
        // then:
        assertThat(results, notNullValue());
        assertThat(results, hasSize(9));
        assertThat(results, hasItem("4.fox"));
    }

    @Test
    public void testUsingTestObserver() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> String.format("%d. %s", index, string));
        // when:
        observable.subscribe(observer);
        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);
        assertThat(observer.values(), hasItem("4. fox"));
    }

    @Test
    public void testFailure() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Exception exception = new RuntimeException("boom!");
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> String.format("%d. %s", index, string))
                .concatWith(Observable.error(exception));
        // when:
        observable.subscribe(observer);
        // then:
        observer.assertError(exception);
        observer.assertNotComplete();
    }

    @Test
    public void testUsingComputationScheduler() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS).zipWith(Observable.range(1, Integer.MAX_VALUE), (string, index) -> String.format("%d. %s", index, string));
        // when:
        observable.subscribeOn(Schedulers.computation()).subscribe(observer);
        // 2017-03-29 注意，这里低于104ms时会导致失败
        await().timeout(1000, MILLISECONDS).until(observer::valueCount, equalTo(9));
        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem("4. fox"));
    }

    @Test
    public void testUsingBlockingCall() {
        // given:
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> String.format("%d. %s", index, string));
        // when:
        Iterable<String> results = observable
                .subscribeOn(Schedulers.computation())
                .blockingIterable();
        // then:
        assertThat(results, notNullValue());
        assertThat(results, iterableWithSize(9));
        assertThat(results, hasItem("4. fox"));
    }

    @Test
    public void testUsingComputationScheduler_awaitTerminalEvent() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> String.format("%d. %s", index, string));
        // when:
        observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);
        observer.awaitTerminalEvent(2000, MILLISECONDS);
        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem("4. fox"));
    }

    @Test
    public void testUsingComputationScheduler_awaitility() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> String.format("%2d. %s", index, string));

        // when:
        observable.subscribeOn(Schedulers.computation()).subscribe(observer);

        await().timeout(2000, MILLISECONDS)
                .until(observer::valueCount, equalTo(9));

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem(" 4. fox"));
    }
}
