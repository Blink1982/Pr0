package com.pr0gramm.app.ui.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.f2prateek.dart.Dart;
import com.pr0gramm.app.ActivityComponent;
import com.pr0gramm.app.Dagger;
import com.pr0gramm.app.ui.dialogs.DialogDismissListener;
import com.pr0gramm.app.util.BackgroundScheduler;
import com.trello.rxlifecycle.FragmentEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatDialogFragment;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

/**
 * A robo fragment that provides lifecycle events as an observable.
 */
public abstract class BaseDialogFragment extends RxAppCompatDialogFragment {
    private final BehaviorSubject<FragmentEvent> lifecycleSubject = BehaviorSubject.create();

    private Unbinder unbinder;

    public final <T> Observable.Transformer<T, T> bindToLifecycleAsync() {
        return observable -> observable
                .subscribeOn(BackgroundScheduler.instance())
                .unsubscribeOn(BackgroundScheduler.instance())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle());
    }

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        lifecycleSubject.onNext(FragmentEvent.ATTACH);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectComponent(Dagger.activityComponent(getActivity()));

        if (getArguments() != null)
            Dart.inject(this, getArguments());

        lifecycleSubject.onNext(FragmentEvent.CREATE);
    }

    protected abstract void injectComponent(ActivityComponent activityComponent);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lifecycleSubject.onNext(FragmentEvent.CREATE_VIEW);
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycleSubject.onNext(FragmentEvent.START);

        // bind dialog. It is only created in on start.
        Dialog dialog = getDialog();
        if (dialog != null) {
            unbinder = ButterKnife.bind(this, dialog);
            onDialogViewCreated();
        }
    }

    protected void onDialogViewCreated() {
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycleSubject.onNext(FragmentEvent.RESUME);
    }

    @Override
    public void onPause() {
        lifecycleSubject.onNext(FragmentEvent.PAUSE);
        super.onPause();
    }

    @Override
    public void onStop() {
        lifecycleSubject.onNext(FragmentEvent.STOP);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);

        lifecycleSubject.onNext(FragmentEvent.DESTROY_VIEW);
        super.onDestroyView();

        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
    }

    @Override
    public void onDestroy() {
        lifecycleSubject.onNext(FragmentEvent.DESTROY);
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        lifecycleSubject.onNext(FragmentEvent.DETACH);
        super.onDetach();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        FragmentActivity activity = getActivity();
        if (activity instanceof DialogDismissListener) {
            // propagate to fragment
            ((DialogDismissListener) activity).onDialogDismissed(this);
        }
    }

    protected Context getThemedContext() {
        Dialog dialog = getDialog();
        return dialog != null ? dialog.getContext() : getContext();
    }

    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception ignored) {
            // i never want that!
        }
    }

    @Override
    public void dismissAllowingStateLoss() {
        try {
            super.dismissAllowingStateLoss();
        } catch (Exception ignored) {
            // i never want that!
        }
    }
}
