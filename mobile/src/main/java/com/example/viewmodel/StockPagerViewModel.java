package com.example.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;

import com.example.entity.LookupEntity;
import com.example.rest.RestHttpLogger;
import com.example.rest.RestResponseHandler;
import com.example.rest.provider.StocksRxServiceProvider;
import com.example.ui.StockPagerView;

import org.alfonz.rest.rx.RestRxManager;
import org.alfonz.rx.AlfonzDisposableObserver;
import org.alfonz.utility.NetworkUtility;
import org.alfonz.view.StatefulLayout;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;
import retrofit2.Response;


public class StockPagerViewModel extends BaseViewModel<StockPagerView>
{
	public final ObservableField<StatefulLayout.State> state = new ObservableField<>();
	public final ObservableArrayList<StockPagerItemViewModel> lookups = new ObservableArrayList<>();

	private RestRxManager mRestRxManager = new RestRxManager(new RestResponseHandler(), new RestHttpLogger());


	@Override
	public void onStart()
	{
		super.onStart();

		// load data
		if(lookups.isEmpty()) loadData();
	}


	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// unsubscribe
		mRestRxManager.disposeAll();
	}


	public void loadData()
	{
		sendLookup("bank");
	}


	private void sendLookup(String input)
	{
		if(NetworkUtility.isOnline(getApplicationContext()))
		{
			if(!mRestRxManager.isRunning(StocksRxServiceProvider.LOOKUP_CALL_TYPE))
			{
				// show progress
				state.set(StatefulLayout.State.PROGRESS);

				// subscribe
				Observable<Response<List<LookupEntity>>> rawObservable = StocksRxServiceProvider.getService().lookup("json", input);
				Observable<Response<List<LookupEntity>>> observable = mRestRxManager.setupRestObservableWithSchedulers(rawObservable, StocksRxServiceProvider.LOOKUP_CALL_TYPE);
				observable.subscribeWith(createLookupObserver());
			}
		}
		else
		{
			// show offline
			state.set(StatefulLayout.State.OFFLINE);
		}
	}


	private DisposableObserver<Response<List<LookupEntity>>> createLookupObserver()
	{
		return AlfonzDisposableObserver.newInstance(
				response ->
				{
					lookups.clear();
					for(LookupEntity e : response.body())
					{
						lookups.add(new StockPagerItemViewModel(e));
					}
				},
				throwable ->
				{
					handleError(mRestRxManager.getHttpErrorMessage(throwable));
					setState(lookups);
				},
				() ->
				{
					setState(lookups);
				}
		);
	}


	private void setState(ObservableArrayList<StockPagerItemViewModel> data)
	{
		if(!data.isEmpty())
		{
			state.set(StatefulLayout.State.CONTENT);
		}
		else
		{
			state.set(StatefulLayout.State.EMPTY);
		}
	}
}
