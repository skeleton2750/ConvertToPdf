package com.androidmarket.pdfcreator.fragment;

import static android.app.Activity.RESULT_OK;
import static com.androidmarket.pdfcreator.Constants.REQUEST_CODE_FOR_WRITE_PERMISSION;
import static com.androidmarket.pdfcreator.Constants.STORAGE_LOCATION;
import static com.androidmarket.pdfcreator.Constants.WRITE_PERMISSIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.androidmarket.pdfcreator.Constants;
import com.androidmarket.pdfcreator.activities.HomeActivity;
import com.androidmarket.pdfcreator.adapter.AdapterEnhancementOptions;
import com.androidmarket.pdfcreator.adapter.AdapterMergeFiles;
import com.androidmarket.pdfcreator.db.DatabaseHelper;
import com.androidmarket.pdfcreator.interfaces.BottomSheetPopulate;
import com.androidmarket.pdfcreator.interfaces.OnItemClickListener;
import com.androidmarket.pdfcreator.interfaces.OnPDFCreatedInterface;
import com.androidmarket.pdfcreator.pdfModel.EnhancementOptionsEntity;
import com.androidmarket.pdfcreator.util.BottomSheetCallback;
import com.androidmarket.pdfcreator.util.BottomSheetUtils;
import com.androidmarket.pdfcreator.util.CommonCodeUtils;
import com.androidmarket.pdfcreator.util.DefaultTextWatcher;
import com.androidmarket.pdfcreator.util.DialogUtils;
import com.androidmarket.pdfcreator.util.ExcelToPDFAsync;
import com.androidmarket.pdfcreator.util.FileUtils;
import com.androidmarket.pdfcreator.util.MorphButtonUtility;
import com.androidmarket.pdfcreator.util.PermissionsUtils;
import com.androidmarket.pdfcreator.util.RealPathUtil;
import com.androidmarket.pdfcreator.util.StringUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.Objects;

import androidmarket.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExceltoPdfFragment extends Fragment implements AdapterMergeFiles.OnClickListener,
        OnPDFCreatedInterface, OnItemClickListener, BottomSheetPopulate {
    private Activity mActivity;
    private FileUtils mFileUtils;
    private Uri mExcelFileUri;
    private String mRealPath;
    private String mPath;
    private BottomSheetBehavior mSheetBehavior;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.lottie_progress)
    LottieAnimationView mLottieProgress;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.tv_excel_file_name_bottom)
    TextView mTextView;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.open_pdf)
    CardView mOpenPdf;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.create_excel_to_pdf)
    CardView mCreateExcelPdf;
//    @BindView(R.id.enhancement_options_recycle_view)
//    RecyclerView mEnhancementOptionsRecycleView;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.bottom_sheet)
    LinearLayout layoutBottomSheet;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.upArrow)
    ImageView mUpArrow;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.layout)
    RelativeLayout mLayout;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.recyclerViewFiles)
    RecyclerView mRecyclerViewFiles;

    private StringUtils mStringUtils;
    private SharedPreferences mSharedPreferences;
    private MorphButtonUtility mMorphButtonUtility;
    private BottomSheetUtils mBottomSheetUtils;
    private boolean mButtonClicked = false;
    private final int mFileSelectCode = 0;
    private MaterialDialog mMaterialDialog;
    private ArrayList<EnhancementOptionsEntity> mEnhancementOptionsEntityArrayList;
    private AdapterEnhancementOptions mAdapterEnhancementOptions;
    private boolean mPasswordProtected = false;
    private String mPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_excelto_pdf, container,
                false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);


        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        ButterKnife.bind(this, rootView);
        //showEnhancementOptions();
        mCreateExcelPdf.setEnabled(false);

        ButterKnife.bind(this, rootView);
        mSheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        mSheetBehavior.setBottomSheetCallback(new BottomSheetCallback(mUpArrow, isAdded()));
        mLottieProgress.setVisibility(View.VISIBLE);
        mBottomSheetUtils.populateBottomSheetWithExcelFiles(this);


        ImageView ivBack = rootView.findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), HomeActivity.class));
                getActivity().finish();
            }
        });

        return rootView;
    }

//    private void showEnhancementOptions() {
//        GridLayoutManager mGridLayoutManager = new GridLayoutManager(mActivity, 1);
//        mEnhancementOptionsRecycleView.setLayoutManager(mGridLayoutManager);
//        mEnhancementOptionsEntityArrayList = MergePdfEnhancementOptionsUtils.getInstance()
//                .getEnhancementOptions(mActivity);
//        mAdapterEnhancementOptions = new AdapterEnhancementOptions(this, mEnhancementOptionsEntityArrayList);
//        mEnhancementOptionsRecycleView.setAdapter(mAdapterEnhancementOptions);
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
        mFileUtils = new FileUtils(mActivity);
        mBottomSheetUtils = new BottomSheetUtils(mActivity);
        mStringUtils = StringUtils.getInstance();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.select_excel_file)
    public void selectExcelFile() {
        if (!mButtonClicked) {
            Uri uri = Uri.parse(Environment.getRootDirectory() + "/");
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setDataAndType(uri, "*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(
                        Intent.createChooser(intent, String.valueOf(R.string.select_file)),
                        mFileSelectCode);
            } catch (android.content.ActivityNotFoundException ex) {
                mStringUtils.showSnackbar(mActivity, R.string.install_file_manager);
            }
            mButtonClicked = true;
        }
    }

    /**
     * This function opens a dialog to enter the file name of
     * the converted file.
     */
    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.create_excel_to_pdf)
    public void openExcelToPdf() {
        if (isStoragePermissionGranted()) {
            openExcelToPdf_();
        } else {
            getRuntimePermissions();
        }
    }

    private void openExcelToPdf_() {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.creating_pdf)
                .content(R.string.enter_file_name)
                .input(getString(R.string.example), null, (dialog, input) -> {
                    if (mStringUtils.isEmpty(input)) {
                        mStringUtils.showSnackbar(mActivity, R.string.snackbar_name_not_blank);
                    } else {
                        final String inputName = input.toString();
                        if (!mFileUtils.isFileExist(inputName + getString(R.string.pdf_ext))) {
                            convertToPdf(inputName);
                        } else {
                            MaterialDialog.Builder builder = DialogUtils.getInstance().createOverwriteDialog(mActivity);
                            builder.onPositive((dialog12, which) -> convertToPdf(inputName))
                                    .onNegative((dialog1, which) -> openExcelToPdf())
                                    .show();
                        }
                    }
                })
                .show();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.open_pdf)
    void openPdf() {
        mFileUtils.openFile(mPath, FileUtils.FileType.e_PDF);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mButtonClicked = false;
        if (requestCode == mFileSelectCode) {
            if (resultCode == RESULT_OK) {
                mExcelFileUri = data.getData();
                mRealPath = RealPathUtil.getInstance().getRealPath(getContext(), mExcelFileUri);
                processUri();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
    private void getRuntimePermissions() {
        if (Build.VERSION.SDK_INT < 29) {
            PermissionsUtils.getInstance().requestRuntimePermissions(this,
                    WRITE_PERMISSIONS,
                    REQUEST_CODE_FOR_WRITE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsUtils.getInstance().handleRequestPermissionsResult(mActivity, grantResults,
                requestCode, REQUEST_CODE_FOR_WRITE_PERMISSION, this::openExcelToPdf_);
    }

    private void processUri() {
        mStringUtils.showSnackbar(mActivity, getResources().getString(R.string.excel_selected));
        String fileName = mFileUtils.getFileName(mExcelFileUri);
        if (fileName != null && !fileName.endsWith(Constants.excelExtension) &&
                !fileName.endsWith(Constants.excelWorkbookExtension)) {
            mStringUtils.showSnackbar(mActivity, R.string.extension_not_supported);
            return;
        }

        fileName = getResources().getString(R.string.excel_selected)
                + fileName;
        mTextView.setText(fileName);
        mTextView.setVisibility(View.VISIBLE);
        mCreateExcelPdf.setEnabled(true);
        mOpenPdf.setVisibility(View.GONE);
    }

    /**
     * This function is required to convert the chosen excel file
     * to PDF.
     *
     * @param mFilename - output PDF name
     */

    private void convertToPdf(String mFilename) {
        String mStorePath = mSharedPreferences.getString(STORAGE_LOCATION,
                mStringUtils.getDefaultStorageLocation());
        mPath = mStorePath + mFilename + mActivity.getString(R.string.pdf_ext);
        new ExcelToPDFAsync(mRealPath, mPath, ExceltoPdfFragment.this, mPasswordProtected, mPassword).execute();

    }

    @Override
    public void onPDFCreationStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void onPDFCreated(boolean success, String path) {
        if (mMaterialDialog != null && mMaterialDialog.isShowing())
            mMaterialDialog.dismiss();
        if (!success) {
            mStringUtils.showSnackbar(mActivity, R.string.error_pdf_not_created);
            mTextView.setVisibility(View.GONE);
            mCreateExcelPdf.setEnabled(false);
            mExcelFileUri = null;
            return;
        }
        mStringUtils.getSnackbarwithAction(mActivity, R.string.snackbar_pdfCreated)
                .setAction(R.string.snackbar_viewAction,
                        v -> mFileUtils.openFile(mPath, FileUtils.FileType.e_PDF))
                .show();
        new DatabaseHelper(mActivity).insertRecord(mPath, mActivity.getString(R.string.created));
        mTextView.setVisibility(View.GONE);
        mOpenPdf.setVisibility(View.VISIBLE);
        mExcelFileUri = null;
        mPasswordProtected = false;
        //showEnhancementOptions();
    }

    @Override
    public void onItemClick(int position) {
        if (!mCreateExcelPdf.isEnabled()) {
            mStringUtils.showSnackbar(mActivity, R.string.no_excel_file);
            return;
        }
        if (position == 0) {
            setPassword();
        }
    }

    private void setPassword() {
        MaterialDialog.Builder builder = DialogUtils.getInstance()
                .createCustomDialogWithoutContent(mActivity, R.string.set_password);
        final MaterialDialog dialog = builder
                .customView(R.layout.custom_dialog, true)
                .neutralText(R.string.remove_dialog)
                .build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        final View neutralAction = dialog.getActionButton(DialogAction.NEUTRAL);
        final EditText passwordInput = Objects.requireNonNull(dialog.getCustomView()).findViewById(R.id.password);
        passwordInput.setText(mPassword);
        passwordInput.addTextChangedListener(watcherImpl(positiveAction));
        if (mStringUtils.isNotEmpty(mPassword)) {
            neutralAction.setOnClickListener(v -> {
                mPassword = null;
                setPasswordIcon(R.drawable.baseline_enhanced_encryption_24);
                mPasswordProtected = false;
                dialog.dismiss();
                mStringUtils.showSnackbar(mActivity, R.string.password_remove);
            });
        }
        dialog.show();
        positiveAction.setEnabled(false);
    }

    private DefaultTextWatcher watcherImpl(View positiveAction) {
        return new DefaultTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable input) {
                if (mStringUtils.isEmpty(input)) {
                    mStringUtils.showSnackbar(mActivity, R.string.snackbar_password_cannot_be_blank);
                } else {
                    mPassword = input.toString();
                    mPasswordProtected = true;
                    setPasswordIcon(R.drawable.baseline_done_24);
                }
            }
        };
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "NotifyDataSetChanged"})
    private void setPasswordIcon(int drawable) {
        mEnhancementOptionsEntityArrayList.get(0).setImage(mActivity.getResources().getDrawable(drawable));
        mAdapterEnhancementOptions.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String path) {
        mSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mExcelFileUri = Uri.parse("file://" + path);
        mRealPath = path;
        processUri();
    }

    @Override
    public void onPopulate(ArrayList<String> paths) {
        CommonCodeUtils.getInstance().populateUtil(mActivity, paths,
                this, mLayout, mLottieProgress, mRecyclerViewFiles);
    }

}
