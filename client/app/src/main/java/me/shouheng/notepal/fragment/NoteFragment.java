package me.shouheng.notepal.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.balysv.materialmenu.MaterialMenuDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.shouheng.commons.activity.PermissionActivity;
import me.shouheng.commons.fragment.CommonFragment;
import me.shouheng.commons.interaction.BackEventResolver;
import me.shouheng.commons.utils.ColorUtils;
import me.shouheng.commons.utils.LogUtils;
import me.shouheng.commons.utils.PermissionUtils;
import me.shouheng.commons.utils.StringUtils;
import me.shouheng.commons.utils.ToastUtils;
import me.shouheng.commons.utils.ViewUtils;
import me.shouheng.easymark.editor.Format;
import me.shouheng.notepal.PalmApp;
import me.shouheng.notepal.R;
import me.shouheng.notepal.activity.MainActivity;
import me.shouheng.notepal.activity.MenuSortActivity;
import me.shouheng.notepal.async.CreateAttachmentTask;
import me.shouheng.notepal.config.Constants;
import me.shouheng.notepal.databinding.FragmentNoteBinding;
import me.shouheng.notepal.dialog.AttachmentPicker;
import me.shouheng.notepal.dialog.LinkInputDialog;
import me.shouheng.notepal.dialog.MathJaxEditor;
import me.shouheng.notepal.dialog.TableInputDialog;
import me.shouheng.notepal.dialog.picker.NotebookPickerDialog;
import me.shouheng.notepal.model.Attachment;
import me.shouheng.notepal.model.Category;
import me.shouheng.notepal.model.Location;
import me.shouheng.notepal.model.Note;
import me.shouheng.notepal.model.enums.ModelType;
import me.shouheng.notepal.util.AttachmentHelper;
import me.shouheng.notepal.util.FileHelper;
import me.shouheng.notepal.util.ModelHelper;
import me.shouheng.notepal.util.preferences.PrefUtils;
import me.shouheng.notepal.viewmodel.BaseViewModel;
import me.shouheng.notepal.viewmodel.CategoryViewModel;
import me.shouheng.notepal.vm.NoteViewModel;
import me.shouheng.notepal.widget.FlowLayout;
import me.shouheng.notepal.widget.MDItemView;

/**
 * The fragment used to edit the note.
 *
 * Created by WngShhng (shouheng2015@gmail) on 2017/5/12.
 * Refactored by WngShhng (shouheng2015@gmail.com) on 2018/11/28.
 */
public class NoteFragment extends CommonFragment<FragmentNoteBinding> implements BackEventResolver {

    /**
     * The key of the argument for {@link Note}. This is a required argument.
     * The note will be used for editing in the fragment. If you don't specify this value,
     * a new note will be created and used as default.
     */
    public final static String EXTRA_KEY_NOTE = "__extra_key_note";

    /**
     * The intent received from the third part which is registered in the Manifest.xml.
     * For example the {@link Intent#ACTION_SEND} etc. The intent will be received in the
     * {@link MainActivity} and send directly to this fragment.
     */
    public final static String EXTRA_KEY_THIRD_PART_INTENT = "__extra_key_third_part_intent";

    /**
     * The action for the note fragment, same as action got from {@link Intent#getAction()}.
     * Actions to use must be defined below, otherwise it won't be handled.
     */
    public final static String EXTRA_KEY_ACTION = "__extra_key_action";

    /**
     * ACTION: To create a new sketch note: create sketch at first then edi the note.
     */
    public final static String ACTION_ADD_SKETCH = "__action_add_sketch";

    /**
     * ACTION: To create a file note: pick files at first then edit the note.
     */
    public final static String ACTION_ADD_FILES = "__action_add_files";

    /**
     * ACTION: To create a photo note: take a photo at first then edit the note.
     */
    public final static String ACTION_TAKE_PHOTO = "__action_take_photo";


    private Note note;
    private List<Category> selections;

    private NoteViewModel viewModel;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_note;
    }

    @Override
    protected void doCreateView(Bundle savedInstanceState) {
        viewModel = getViewModel(NoteViewModel.class);
        handleArguments();
        configToolbar();
        configViews();
    }

    private void handleArguments() {
        Bundle args = getArguments();

        /* Check the existence of the note to edit. */
        if (args == null) {
            if (getActivity() != null) getActivity().finish();
        } else {
            if (!args.containsKey(EXTRA_KEY_NOTE)) {
                note=
            }
        }

        if (args == null
                || !args.containsKey(EXTRA_KEY_NOTE)
                || (note = (Note) args.getSerializable(EXTRA_KEY_NOTE)) == null) {
            ToastUtils.makeToast(R.string.text_no_such_note);

            return;
        }

        if (args.containsKey(EXTRA_KEY_THIRD_PART_INTENT)) {
            Intent intent = args.getParcelable(EXTRA_KEY_THIRD_PART_INTENT);
        }



        // Handle args for intent from third part
        if (args.getBoolean(EXTRA_IS_THIRD_PART)) {
            handleThirdPart();
        } else if(Constants.ACTION_ADD_SKETCH.equals(args.getString(EXTRA_ACTION))) {
            if (getActivity() != null) {
                PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> AttachmentHelper.sketch(this));
            }
        } else if (Constants.ACTION_TAKE_PHOTO.equals(args.getString(EXTRA_ACTION))) {
            if (getActivity() != null) {
                PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> AttachmentHelper.capture(this));
            }
        } else if (Constants.ACTION_ADD_FILES.equals(args.getString(EXTRA_ACTION))) {
            if (getActivity() != null) {
                PermissionUtils.checkStoragePermission((PermissionActivity) getActivity(), () -> AttachmentHelper.pickFiles(this));
            }
        } else {
            // The cases above is new model, don't need to fetch data.
            fetchData(note);
        }
    }

    private void handleThirdPart() {
//        if (!(getActivity() instanceof OnNoteInteractListener)) return;
//
//        Intent intent = ((OnNoteInteractListener) getActivity()).getIntentForThirdPart();
//
//        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
//        note.setTitle(title);
//
//        String content = intent.getStringExtra(Intent.EXTRA_TEXT);
//        if (!TextUtils.isEmpty(content)) {
//            content = content.replace("\t", TAB_REPLACEMENT);
//        }
//        note.setContent(content);
//
//        // Single attachment data
//        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
//
//        // Due to the fact that Google Now passes intent as text but with
//        // audio recording attached the case must be handled in specific way
//        if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(intent.getAction())) {
//            new CreateAttachmentTask(this, uri, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//        }
//
//        // Multiple attachment data
//        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
//        if (uris != null) {
//            for (Uri uriSingle : uris) {
//                new CreateAttachmentTask(this, uriSingle, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            }
//        }
    }
    // endregion

    private void configToolbar() {
        if (getContext() == null || getActivity() == null) return;

        getBinding().toolbar.setNavigationIcon(materialMenu);
        ((AppCompatActivity) getActivity()).setSupportActionBar(getBinding().toolbar);
        ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle("");
            setStatusBarColor(getResources().getColor(isDarkTheme() ? R.color.dark_theme_foreground : R.color.md_grey_500));
        }
    }

    // region fetch data
    private void fetchData(Note note) {
        fetchNoteContent(note);

        fetchCategories(note);

        fetchNotebook(note);
    }

    private void fetchNoteContent(Note note) {
        // 恢复现场，不需要重新加载数据
        if (!TextUtils.isEmpty(note.getContent())
                && getArguments() != null
                && getArguments().containsKey(KEY_ARGS_RESTORE)
                && getArguments().getBoolean(KEY_ARGS_RESTORE)) {
            return;
        }

        attachmentViewModel.readNoteContent(note).observe(this, contentResource -> {
            if (contentResource == null) {
                ToastUtils.makeToast(R.string.text_failed_to_load_data);
                return;
            }
            switch (contentResource.status) {
                case SUCCESS:
                    note.setContent(contentResource.data);
                    getBinding().etContent.setTag(true);
                    getBinding().etContent.setText(note.getContent());
                    break;
                case FAILED:
                    ToastUtils.makeToast(R.string.note_failed_to_read_file);
                    break;
            }
        });
    }

    private void fetchCategories(Note note) {
        categoryViewModel.getCategories(note).observe(this, listResource -> {
            if (listResource == null) {
                ToastUtils.makeToast(R.string.text_failed_to_load_data);
                return;
            }
            switch (listResource.status) {
                case SUCCESS:
                    selections = listResource.data;
                    addTagsToLayout(CategoryViewModel.getTagsName(listResource.data));
                    break;
            }
        });
    }

    private void fetchNotebook(Note note) {
        notebookViewModel.get(note.getParentCode()).observe(this, notebookResource -> {
            if (notebookResource == null) {
                ToastUtils.makeToast(R.string.text_failed_to_load_data);
                return;
            }
            switch (notebookResource.status) {
                case SUCCESS:
                    if (notebookResource.data != null) {
                        getBinding().tvFolder.setText(notebookResource.data.getTitle());
                        getBinding().tvFolder.setTextColor(notebookResource.data.getColor());
                    }
                    break;
            }
        });
    }
    // endregion

    // region Config main board
    private void configViews() {
        getBinding().etTitle.setText(TextUtils.isEmpty(note.getTitle()) ? "" : note.getTitle());
        getBinding().etTitle.setTextColor(accentColor());
        getBinding().etTitle.addTextChangedListener(titleWatcher);

        getBinding().etContent.setText(note.getContent());
        getBinding().etContent.addTextChangedListener(contentWatcher);

        getBinding().llFolder.setOnClickListener(v -> showNotebookPicker());

        getBinding().rlBottomEditors.setVisibility(View.GONE);

        int[] ids = new int[]{R.id.iv_redo, R.id.iv_undo, R.id.iv_insert_picture, R.id.iv_insert_link, R.id.iv_table};
        for (int id : ids) {
            getBinding().getRoot().findViewById(id).setOnClickListener(this::onFormatClick);
        }

        addBottomMenus();

        getBinding().ivEnableFormat.setOnClickListener(v -> switchFormat());
        getBinding().ivSetting.setOnClickListener(v -> MenuSortActivity.start(NoteFragment.this, REQ_MENU_SORT));

        getBinding().fssv.getFastScrollDelegate().setThumbSize(16, 40);
        getBinding().fssv.getFastScrollDelegate().setThumbDynamicHeight(false);
        if (getContext() != null) {
            getBinding().fssv.getFastScrollDelegate().setThumbDrawable(PalmApp.getDrawableCompact(isDarkTheme() ?
                    R.drawable.fast_scroll_bar_dark : R.drawable.fast_scroll_bar_light));
        }
    }

    private void addBottomMenus() {
        getBinding().llContainer.removeAllViews();
        int dp12 = ViewUtils.dp2Px(getContext(), 12);
        List<Format> markdownFormats = PrefUtils.getInstance().getMarkdownFormats();
        for (Format markdownFormat : markdownFormats) {
            MDItemView mdItemView = new MDItemView(getContext());
            mdItemView.setFormat(markdownFormat);
            mdItemView.setPadding(dp12, dp12, dp12, dp12);
            getBinding().llContainer.addView(mdItemView);
            mdItemView.setOnClickListener(v -> {
                if (markdownFormat == Format.CHECKBOX_FILLED
                        || markdownFormat == Format.CHECKBOX) {
                    getBinding().etContent.useFormat(markdownFormat);
                } else if (markdownFormat == Format.MATH_JAX) {
                    showMarkJaxEditor();
                } else {
                    getBinding().etContent.useFormat(markdownFormat);
                }
            });
        }
    }

    private TextWatcher titleWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            note.setTitle(editable.toString());
            setContentChanged();
//            updateCharsInfo();
        }
    };

    private TextWatcher contentWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            // Ignore the text change if the tag is true
            if (getBinding().etContent.getTag() != null ||
                    (getBinding().etContent.getTag() instanceof Boolean
                            && ((boolean) getBinding().etContent.getTag()))) {
                getBinding().etContent.setTag(null);
            } else {
                note.setContent(s.toString());
                setContentChanged();
            }
        }
    };

    private void onFormatClick(View v) {
        switch (v.getId()){
            case R.id.iv_undo:getBinding().etContent.undo();break;
            case R.id.iv_redo:getBinding().etContent.redo();break;
            case R.id.iv_insert_picture:showAttachmentPicker();break;
            case R.id.iv_insert_link:showLinkEditor();break;
            case R.id.iv_table:showTableEditor();break;
        }
    }

    private void switchFormat() {
        boolean rlBottomVisible = getBinding().rlBottomEditors.getVisibility() == View.VISIBLE;
        getBinding().rlBottomEditors.setVisibility(rlBottomVisible ? View.GONE : View.VISIBLE);
        getBinding().ivEnableFormat.setImageDrawable(ColorUtils.tintDrawable(
                R.drawable.ic_text_format_black_24dp,
                rlBottomVisible ? Color.WHITE : primaryColor()));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getBinding().ivEnableFormat.getHeight() * (rlBottomVisible ? 1 : 2));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        getBinding().rlBottom.setLayoutParams(params);
    }

    // TODO
    private void addImageLink() {
        LinkInputDialog.getInstance((title, link) -> {}
//                getBinding().etContent.addLinkEffect(MarkdownFormat.ATTACHMENT, title, link)
        ).show(Objects.requireNonNull(getFragmentManager()), "Link Image");
    }

    // TODO
    private void showMarkJaxEditor() {
        MathJaxEditor.newInstance((exp, isSingleLine) ->
                getBinding().etContent.useFormat(Format.MATH_JAX)
        ).show(Objects.requireNonNull(getFragmentManager()), "MATH JAX EDITOR");
    }

    private void showTableEditor() {
        TableInputDialog.getInstance((rowsStr, colsStr) -> {
            int rows = StringUtils.parseInteger(rowsStr, 3);
            int cols = StringUtils.parseInteger(colsStr, 3);
            getBinding().etContent.useFormat(Format.TABLE);
        }).show(Objects.requireNonNull(getFragmentManager()), "TABLE INPUT");
    }

    // TODO
    private void showLinkEditor() {
        LinkInputDialog.getInstance((title, link) ->
                getBinding().etContent.useFormat(Format.LINK)
        ).show(Objects.requireNonNull(getFragmentManager()), "LINK INPUT");
    }

    private void showAttachmentPicker() {
        new AttachmentPicker.Builder(this)
                .setRecordVisible(false)
                .setVideoVisible(false)
                .setAddLinkVisible(true)
                .setFilesVisible(true)
                .setOnAddNetUriSelectedListener(this::addImageLink)
                .build().show(Objects.requireNonNull(getFragmentManager()), "Attachment picker");
    }

    // endregion

    private void showNotebookPicker() {
        NotebookPickerDialog.newInstance().setOnItemSelectedListener((dialog, value, position) -> {
            note.setParentCode(value.getCode());
            note.setTreePath(value.getTreePath() + "|" + value.getCode());
            getBinding().tvFolder.setText(value.getTitle());
            getBinding().tvFolder.setTextColor(value.getColor());
            setContentChanged();
            dialog.dismiss();
        }).show(Objects.requireNonNull(getFragmentManager()), "NOTEBOOK_PICKER");
    }

    @Override
    protected FlowLayout getTagsLayout() {
        return null;
    }

    @Override
    protected void onGetSelectedCategories(List<Category> categories) {
        String tagsName = CategoryViewModel.getTagsName(categories);
        selections = categories;
        note.setTags(CategoryViewModel.getTags(categories));
        note.setTagsName(tagsName);
        addTagsToLayout(tagsName);
        setContentChanged();
    }

    @Override
    protected void onGetLocation(Location location) { }

    @Override
    protected void onFailedGetAttachment(Attachment attachment) {
        ToastUtils.makeToast(R.string.failed_to_save_attachment);
    }

    @Override
    protected void onGetAttachment(@NonNull Attachment attachment) {
        attachment.setModelCode(note.getCode());
        attachment.setModelType(ModelType.NOTE);
        attachmentViewModel.saveModel(attachment).observe(this, LogUtils::d);

        String title = FileHelper.getNameFromUri(getContext(), attachment.getUri());
        if (TextUtils.isEmpty(title)) title = getString(R.string.text_attachment);

        // TODO the attachment location
        if (Constants.MIME_TYPE_IMAGE.equalsIgnoreCase(attachment.getMineType())
                || Constants.MIME_TYPE_SKETCH.equalsIgnoreCase(attachment.getMineType())) {
            getBinding().etContent.useFormat(Format.IMAGE);
        } else {
            getBinding().etContent.useFormat(Format.IMAGE);
        }
    }

    @Override
    protected void beforeSaveOrUpdate(BeforePersistEventHandler handler) {
        String noteContent = getBinding().etContent.getText().toString();
        note.setContent(noteContent);

        // Get note title from title editor or note content
        String inputTitle = getBinding().etTitle.getText().toString();
        note.setTitle(ModelHelper.getNoteTitle(inputTitle, noteContent));

        // Get preview image from note content
        note.setPreviewImage(ModelHelper.getNotePreviewImage(noteContent));

        note.setPreviewContent(ModelHelper.getNotePreview(noteContent));

        attachmentViewModel.writeNoteContent(note).observe(this, attachmentResource -> {
            if (attachmentResource == null) {
                ToastUtils.makeToast(R.string.text_error_when_save);
                return;
            }
            switch (attachmentResource.status) {
                case SUCCESS:
                    if (attachmentResource.data != null) {
                        note.setContentCode(attachmentResource.data.getCode());
                    }
                    handler.onGetEventResult(true);
                    break;
                case FAILED:
                    ToastUtils.makeToast(R.string.text_error_when_save);
                    handler.onGetEventResult(false);
                    break;
            }
        });
    }

    @Override
    protected void afterSaveOrUpdate() {
//        super.afterSaveOrUpdate();
//        materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
//        String content = getBinding().etContent.getText().toString();
//        if (TextUtils.isEmpty(content)) content = "  ";
//        note.setContent(content);
//
//        Bundle args = getArguments();
//        if (args != null && (args.getBoolean(EXTRA_IS_THIRD_PART)
//                || Constants.ACTION_ADD_SKETCH.equals(args.getString(EXTRA_ACTION))
//                || Constants.ACTION_ADD_FILES.equals(args.getString(EXTRA_ACTION))
//                || Constants.ACTION_TAKE_PHOTO.equals(args.getString(EXTRA_ACTION)))) {
//            sendNoteChangeBroadcast();
//        }
    }

    @Override
    protected BaseViewModel<Note> getViewModel() {
        return noteViewModel;
    }

    @Override
    protected Note getModel() {
        return note;
    }

    @Override
    protected void setContentChanged() {
        if (!isContentChanged()) {
            super.setContentChanged();
            materialMenu.animateIconState(MaterialMenuDrawable.IconState.CHECK);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_editor_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if (isContentChanged()) saveOrUpdateData(null);
                else setResult();
                break;
            case R.id.action_preview:
//                note.setTitle(TextUtils.isEmpty(getBinding().etTitle.getText().toString()) ?
//                        "" : getBinding().etTitle.getText().toString());
//                String content = getBinding().etContent.getText().toString();
//                if (TextUtils.isEmpty(content)) content = "  ";
//                note.setContent(content);
//                ContainerActivity.open(NoteViewFragment.class)
//                        .put(NoteViewFragment.ARGS_KEY_NOTE, (Serializable) note)
//                        .put(NoteViewFragment.ARGS_KEY_IS_PREVIEW, true)
//                        .launch(getActivity());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQ_MENU_SORT:
                    addBottomMenus();
                    break;
            }
        }
    }

    private void sendNoteChangeBroadcast() {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_NOTE_CHANGE_BROADCAST);
        if (getContext() != null) getContext().sendBroadcast(intent);
    }

    @Override
    public void resolve() {
        onBack();
    }

    @Override
    protected String umengPageName() {
        return "NoteEditFragment";
    }

    public interface OnNoteInteractListener {
        Intent getIntentForThirdPart();
    }
}
