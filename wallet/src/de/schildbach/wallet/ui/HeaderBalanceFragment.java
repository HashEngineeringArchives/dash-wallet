/*
 * Copyright 2019 Dash Core Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schildbach.wallet.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.wallet.Wallet;
import org.dash.wallet.common.Configuration;
import org.dash.wallet.common.ui.CurrencyTextView;
import org.dash.wallet.common.util.GenericUtils;

import javax.annotation.Nullable;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.rates.ExchangeRate;
import de.schildbach.wallet.rates.ExchangeRatesViewModel;
import de.schildbach.wallet.service.BlockchainState;
import de.schildbach.wallet.service.BlockchainStateLoader;
import de.schildbach.wallet_test.R;

public final class HeaderBalanceFragment extends Fragment {

    private WalletApplication application;
    private AbstractBindServiceActivity activity;
    private Configuration config;
    private Wallet wallet;
    private LoaderManager loaderManager;

    private View viewBalance;
    private CurrencyTextView viewBalanceDash;
    private CurrencyTextView viewBalanceLocal;

    private boolean showLocalBalance;

    private ExchangeRatesViewModel exchangeRatesViewModel;

    @Nullable
    private Coin balance = null;
    @Nullable
    private ExchangeRate exchangeRate = null;

    private static final int ID_BALANCE_LOADER = 0;
    private static final int ID_BLOCKCHAIN_STATE_LOADER = 1;

    private boolean initComplete = false;

    private Handler autoLockHandler = new Handler();

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        this.activity = (AbstractBindServiceActivity) activity;
        this.application = (WalletApplication) activity.getApplication();
        this.config = application.getConfiguration();
        this.wallet = application.getWallet();
        this.loaderManager = LoaderManager.getInstance(this);

        showLocalBalance = getResources().getBoolean(R.bool.show_local_balance);
    }

    @Override
    public void onActivityCreated(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        exchangeRatesViewModel = ViewModelProviders.of(this).get(ExchangeRatesViewModel.class);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.header_balance_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewBalance = view.findViewById(R.id.wallet_balance);

        viewBalanceDash = view.findViewById(R.id.wallet_balance_dash);
        viewBalanceDash.setApplyMarkup(false);

        viewBalanceLocal = view.findViewById(R.id.wallet_balance_local);
        viewBalanceLocal.setInsignificantRelativeSize(1);
        viewBalanceLocal.setStrikeThru(!Constants.IS_PROD_BUILD);

        viewBalance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(getActivity() instanceof ExchangeRatesActivity)) {
                    showExchangeRatesActivity();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        loaderManager.initLoader(ID_BALANCE_LOADER, null, balanceLoaderCallbacks);
        if (!initComplete) {
            loaderManager.initLoader(ID_BLOCKCHAIN_STATE_LOADER, null, blockchainStateLoaderCallbacks);
            initComplete = true;
        } else {
            loaderManager.restartLoader(ID_BLOCKCHAIN_STATE_LOADER, null, blockchainStateLoaderCallbacks);
        }

        exchangeRatesViewModel.getRate(config.getExchangeCurrencyCode()).observe(this,
                new Observer<ExchangeRate>() {
                    @Override
                    public void onChanged(ExchangeRate rate) {
                        if (rate != null) {
                            exchangeRate = rate;
                            updateView();
                        }
                    }
                });

        updateView();
    }

    @Override
    public void onPause() {
        loaderManager.destroyLoader(ID_BLOCKCHAIN_STATE_LOADER);
        loaderManager.destroyLoader(ID_BALANCE_LOADER);

        autoLockHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    private void updateView() {
        if (!isAdded()) {
            return;
        }

        viewBalance.setVisibility(View.VISIBLE);

        if (!showLocalBalance)
            viewBalanceLocal.setVisibility(View.GONE);

        if (balance != null) {
            viewBalanceDash.setVisibility(View.VISIBLE);
            viewBalanceDash.setFormat(config.getFormat().noCode());
            viewBalanceDash.setAmount(balance);

            if (showLocalBalance) {
                if (exchangeRate != null) {
                    org.bitcoinj.utils.ExchangeRate rate = new org.bitcoinj.utils.ExchangeRate(Coin.COIN,
                            exchangeRate.getFiat());
                    final Fiat localValue = rate.coinToFiat(balance);
                    viewBalanceLocal.setVisibility(View.VISIBLE);
                    String currencySymbol = GenericUtils.currencySymbol(exchangeRate.getCurrencyCode());
                    viewBalanceLocal.setFormat(Constants.LOCAL_FORMAT.code(0, currencySymbol));
                    viewBalanceLocal.setAmount(localValue);
                } else {
                    viewBalanceLocal.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            viewBalanceDash.setVisibility(View.INVISIBLE);
        }
        viewBalance.setVisibility(View.VISIBLE);

        activity.invalidateOptionsMenu();
    }

    private void showExchangeRatesActivity() {
        Intent intent = new Intent(getActivity(), ExchangeRatesActivity.class);
        getActivity().startActivity(intent);
    }

    private final LoaderManager.LoaderCallbacks<BlockchainState> blockchainStateLoaderCallbacks = new LoaderManager.LoaderCallbacks<BlockchainState>() {
        @Override
        public Loader<BlockchainState> onCreateLoader(final int id, final Bundle args) {
            return new BlockchainStateLoader(activity);
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<BlockchainState> loader, final BlockchainState blockchainState) {
            updateView();
        }

        @Override
        public void onLoaderReset(@NonNull final Loader<BlockchainState> loader) {
        }
    };

    private final LoaderManager.LoaderCallbacks<Coin> balanceLoaderCallbacks = new LoaderManager.LoaderCallbacks<Coin>() {
        @Override
        public Loader<Coin> onCreateLoader(final int id, final Bundle args) {
            return new WalletBalanceLoader(activity, wallet);
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<Coin> loader, final Coin balance) {
            HeaderBalanceFragment.this.balance = balance;

            updateView();
        }

        @Override
        public void onLoaderReset(@NonNull final Loader<Coin> loader) {
        }
    };
}
