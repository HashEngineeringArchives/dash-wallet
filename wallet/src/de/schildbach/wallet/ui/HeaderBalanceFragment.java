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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.amulyakhare.textdrawable.TextDrawable;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.wallet.Wallet;
import org.dash.wallet.common.Configuration;
import org.dash.wallet.common.ui.CurrencyTextView;
import org.dash.wallet.common.util.GenericUtils;

import javax.annotation.Nullable;

import de.schildbach.wallet.AppDatabase;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.BlockchainIdentityBaseData;
import de.schildbach.wallet.data.BlockchainIdentityData;
import de.schildbach.wallet.data.BlockchainState;
import de.schildbach.wallet.rates.ExchangeRate;
import de.schildbach.wallet.rates.ExchangeRatesViewModel;
import de.schildbach.wallet_test.R;

public final class HeaderBalanceFragment extends Fragment {

    private WalletApplication application;
    private AbstractBindServiceActivity activity;
    private Configuration config;
    private Wallet wallet;
    private LoaderManager loaderManager;

    private Boolean hideBalance;
    private View showBalanceButton;
    private TextView hideShowBalanceHint;
    private TextView caption;
    private View view;
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
    private BlockchainState blockchainState;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        this.activity = (AbstractBindServiceActivity) activity;
        this.application = (WalletApplication) activity.getApplication();
        this.config = application.getConfiguration();
        this.wallet = application.getWallet();
        this.loaderManager = LoaderManager.getInstance(this);
        hideBalance = config.getHideBalance();

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

        caption = view.findViewById(R.id.caption);
        hideShowBalanceHint = view.findViewById(R.id.hide_show_balance_hint);
        this.view = view;
        showBalanceButton = view.findViewById(R.id.show_balance_button);

        viewBalanceDash = view.findViewById(R.id.wallet_balance_dash);
        viewBalanceDash.setApplyMarkup(false);

        viewBalanceLocal = view.findViewById(R.id.wallet_balance_local);
        viewBalanceLocal.setInsignificantRelativeSize(1);
        viewBalanceLocal.setStrikeThru(!Constants.IS_PROD_BUILD);

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBalance = !hideBalance;
                updateView();
            }
        });

        AppDatabase.getAppDatabase().blockchainStateDao().load().observe(getViewLifecycleOwner(), new Observer<de.schildbach.wallet.data.BlockchainState>() {
            @Override
            public void onChanged(de.schildbach.wallet.data.BlockchainState blockchainState) {
                HeaderBalanceFragment.this.blockchainState = blockchainState;
                updateView();
            }
        });

        AppDatabase.getAppDatabase().blockchainIdentityDataDao().loadBase().observe(getViewLifecycleOwner(), new Observer<BlockchainIdentityBaseData>() {
            @Override
            public void onChanged(BlockchainIdentityBaseData blockchainIdentityData) {
                if (blockchainIdentityData != null
                        && blockchainIdentityData.getCreationState().ordinal() >= BlockchainIdentityData.CreationState.DONE.ordinal()) {
                    String firstLetter = blockchainIdentityData.getUsername().substring(0, 1);
                    setDefaultUserAvatar(firstLetter.toUpperCase());
                } else {
                    setDefaultUserAvatar(null);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        loaderManager.initLoader(ID_BALANCE_LOADER, null, balanceLoaderCallbacks);
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

        if (config.getHideBalance()) {
            hideBalance = true;
        }

        updateView();
    }

    @Override
    public void onPause() {
        loaderManager.destroyLoader(ID_BLOCKCHAIN_STATE_LOADER);
        loaderManager.destroyLoader(ID_BALANCE_LOADER);

        autoLockHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    private void setDefaultUserAvatar(String letters) {
        ImageView dashpayUserAvatar = view.findViewById(R.id.dashpay_user_avatar);
        if (letters == null) {
            // there is no username, so hide the image
            dashpayUserAvatar.setVisibility(View.GONE);
            return;
        }
        dashpayUserAvatar.setVisibility(View.VISIBLE);
        float[] hsv = new float[3];
        //Ascii codes for A: 65 - Z: 90, 0: 48 - 9: 57
        float firstChar = letters.charAt(0);
        float charIndex;
        if (firstChar <= 57) { //57 == '9' in Ascii table
            charIndex = (firstChar - 48f) / 36f; // 48 == '0', 36 == total count of supported
        } else {
            charIndex = (firstChar - 65f + 10f) / 36f; // 65 == 'A', 10 == count of digits
        }
        hsv[0] = charIndex * 360f;
        hsv[1] = 0.3f;
        hsv[2] = 0.6f;
        int bgColor = Color.HSVToColor(hsv);
        final TextDrawable defaultAvatar = TextDrawable.builder().beginConfig().textColor(Color.WHITE)
                .useFont(ResourcesCompat.getFont(getContext(), R.font.montserrat_regular))
                .endConfig().buildRound(letters, bgColor);
        dashpayUserAvatar.setBackground(defaultAvatar);
    }

    private void updateView() {
        View balances = view.findViewById(R.id.balances_layout);
        TextView walletBalanceSyncMessage = view.findViewById(R.id.wallet_balance_sync_message);

        if (hideBalance) {
            caption.setText(R.string.home_balance_hidden);
            hideShowBalanceHint.setText(R.string.home_balance_show_hint);
            balances.setVisibility(View.INVISIBLE);
            walletBalanceSyncMessage.setVisibility(View.GONE);
            showBalanceButton.setVisibility(View.VISIBLE);
            return;
        }
        balances.setVisibility(View.VISIBLE);
        caption.setText(R.string.home_available_balance);
        hideShowBalanceHint.setText(R.string.home_balance_hide_hint);
        showBalanceButton.setVisibility(View.GONE);

        if (!isAdded()) {
            return;
        }

        if (blockchainState != null && blockchainState.isSynced()) {
            balances.setVisibility(View.VISIBLE);
            walletBalanceSyncMessage.setVisibility(View.GONE);
        } else {
            balances.setVisibility(View.INVISIBLE);
            walletBalanceSyncMessage.setVisibility(View.VISIBLE);
            return;
        }

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

        activity.invalidateOptionsMenu();
    }

    private void showExchangeRatesActivity() {
        Intent intent = new Intent(getActivity(), ExchangeRatesActivity.class);
        getActivity().startActivity(intent);
    }

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
