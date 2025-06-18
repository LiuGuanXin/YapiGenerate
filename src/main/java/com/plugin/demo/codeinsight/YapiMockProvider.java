package com.plugin.demo.codeinsight;

import com.intellij.codeInsight.codeVision.*;
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry;
import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry;
import com.intellij.codeInsight.hints.InlayHintsUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import kotlin.Pair;
import kotlin.Unit;
import org.jetbrains.annotations.Nls;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Liu Guangxin
 * @date 2025/6/12 16:40
 */
public class YapiMockProvider implements CodeVisionProvider<Unit> {

    public static final String GROUP_ID = "com.demo";
    public static final String ID = "generateYapiMock";
    public static final String NAME = "文档生成(同时生成Mock数据)";

    private static final Key<Long> MODIFICATION_STAMP_KEY = Key.create("myPlugin.modificationStamp");
    private static final Key<Integer> MODIFICATION_STAMP_COUNT_KEY = KeyWithDefaultValue.create("myPlugin.modificationStampCount", 0);
    private static final int MAX_MODIFICATION_STAMP_COUNT = 4;

    @NotNull
    @Override
    public CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Top;
    }

    @NotNull
    @Override
    public String getGroupId() {
        return GROUP_ID;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return List.of(
                new CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter("YapiProvider"),
                new CodeVisionRelativeOrdering.CodeVisionRelativeOrderingBefore("YapiSendProvider"));
    }


    @NotNull
    @Override
    public CodeVisionState computeCodeVision(@NotNull Editor editor, @Nullable Unit uiData) {
        List<PsiMethod> psiMethods = getPsiMethods(editor);
        List<Pair<TextRange, CodeVisionEntry>> lenses = new ArrayList<>();
        for (PsiMethod psiMethod : psiMethods) {
            TextRange range = InlayHintsUtils.INSTANCE.getTextRangeWithoutLeadingCommentsAndWhitespaces(psiMethod);
            MyClickHandler handler = new MyClickHandler(psiMethod, true);
            CodeVisionEntry entry = new ClickableTextCodeVisionEntry(getName(), getId(), handler, null, getName(), getName(), List.of());
            lenses.add(new Pair<>(range, entry));
        }
        return new CodeVisionState.Ready(lenses);
    }

	private List<PsiMethod> getPsiMethods(Editor editor) {
		return ApplicationManager.getApplication().runReadAction((Computable<List<PsiMethod>>) () -> {
			List<PsiMethod> result = new ArrayList<>();
			PsiFile psiFile = PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject()))
					.getPsiFile(editor.getDocument());
			if (psiFile == null) return result;

			SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(psiFile);
			for (PsiElement element : traverser) {
				if (!(element instanceof PsiMethod)) continue;
				if (!InlayHintsUtils.isFirstInLine(element)) continue;

				PsiMethod method = (PsiMethod) element;
				if (isControllerMethod(method)) {
					result.add(method);
				}
			}
			return result;
		});
	}


    @Override
    public void handleClick(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull CodeVisionEntry entry) {
        if (entry instanceof CodeVisionPredefinedActionEntry) {
            ((CodeVisionPredefinedActionEntry) entry).onClick(editor);
        }
    }

    @Override
    public void handleExtraAction(@NotNull Editor editor, @NotNull TextRange textRange, @NotNull String s) {

    }

    @Override
    public Unit precomputeOnUiThread(@NotNull Editor editor) {
        return Unit.INSTANCE; // 避免 computeCodeVision 参数为 null
    }

    @Override
    public boolean shouldRecomputeForEditor(@NotNull Editor editor, @Nullable Unit uiData) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            if (editor.isDisposed() || !editor.isInsertMode()) return false;
            Project project = editor.getProject();
            if (project == null) return false;

            Document document = editor.getDocument();
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null || !"JAVA".equalsIgnoreCase(psiFile.getLanguage().getID())) return false;

            Long prevStamp = MODIFICATION_STAMP_KEY.get(editor);
            long nowStamp = getDocumentStamp(document);
            if (prevStamp == null || prevStamp != nowStamp) {
                Integer count = MODIFICATION_STAMP_COUNT_KEY.get(editor);
                if (count + 1 < MAX_MODIFICATION_STAMP_COUNT) {
                    MODIFICATION_STAMP_COUNT_KEY.set(editor, count + 1);
                    return true;
                } else {
                    MODIFICATION_STAMP_COUNT_KEY.set(editor, 0);
                    MODIFICATION_STAMP_KEY.set(editor, nowStamp);
                    return true;
                }
            }
            return false;
        });
    }

    private static long getDocumentStamp(@NotNull Document document) {
        if (document instanceof DocumentEx) {
            return ((DocumentEx) document).getModificationSequence();
        }
        return document.getModificationStamp();
    }

    private boolean isControllerMethod(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return false;

        // 检查类注解
        for (PsiAnnotation annotation : containingClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (
                    qualifiedName.contains("Controller") ||
                            qualifiedName.contains("RestController"))) {
                return true;
            }
        }

        // 检查方法注解
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (
                    qualifiedName.contains("RequestMapping") ||
                            qualifiedName.contains("GetMapping") ||
                            qualifiedName.contains("PostMapping") ||
                            qualifiedName.contains("PutMapping") ||
                            qualifiedName.contains("DeleteMapping") ||
                            qualifiedName.contains("PatchMapping"))) {
                return true;
            }
        }

        return false;
    }
}