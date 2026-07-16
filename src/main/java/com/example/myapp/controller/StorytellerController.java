package com.example.myapp.controller;

import com.example.myapp.controller.api.StorytellerUploadApiController;
import com.example.myapp.dto.TaleForm;
import com.example.myapp.model.Category;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.service.CategoryService;
import com.example.myapp.service.CommentService;
import com.example.myapp.service.FileStorageService;
import com.example.myapp.service.TaleService;
import com.example.myapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/storyteller")
public class StorytellerController {

    private final TaleService taleService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final FileStorageService storage;
    private final CommentService commentService;

    public StorytellerController(TaleService taleService,
                                 CategoryService categoryService,
                                 UserService userService,
                                 FileStorageService storage,
                                 CommentService commentService) {
        this.taleService = taleService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.storage = storage;
        this.commentService = commentService;
    }

    @GetMapping("/tales")
    public String myTales(Model model, Principal principal) {
        model.addAttribute("tales", taleService.findByStoryteller(currentUser(principal)));
        return "storyteller/tales";
    }

    @GetMapping("/comments")
    public String comments(Model model, Principal principal) {
        User user = currentUser(principal);
        var comments = commentService.forStoryteller(user);
        model.addAttribute("comments", comments);
        // Mark after load so the template can still show "جدید" for this visit
        commentService.markAllSeenForStoryteller(user);
        return "storyteller/comments";
    }

    @GetMapping("/tales/new")
    public String newForm(@ModelAttribute("form") TaleForm form, Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("tale", null);
        return "storyteller/form";
    }

    @PostMapping("/tales")
    public String create(@Valid @ModelAttribute("form") TaleForm form,
                         BindingResult result,
                         Model model,
                         Principal principal,
                         HttpSession session) {
        boolean hasPreUploaded = form.getAudioFilename() != null && !form.getAudioFilename().isBlank();
        boolean hasMultipart = form.getAudio() != null && !form.getAudio().isEmpty();
        if (!hasPreUploaded && !hasMultipart) {
            result.rejectValue("audio", "required", "لطفاً صدای قصه را انتخاب یا ضبط کنید");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("tale", null);
            return "storyteller/form";
        }
        try {
            Tale tale = new Tale();
            tale.setStoryteller(currentUser(principal));
            applyForm(tale, form, true, session);
            taleService.save(tale);
        } catch (IllegalArgumentException e) {
            result.reject("storage", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("tale", null);
            return "storyteller/form";
        }
        return "redirect:/storyteller/tales?created";
    }

    @GetMapping("/tales/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        Tale tale = editableTale(id, principal);
        TaleForm form = new TaleForm();
        form.setTitle(tale.getTitle());
        form.setDescription(tale.getDescription());
        form.setCategoryIds(tale.getCategories().stream().map(Category::getId).collect(Collectors.toList()));
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("tale", tale);
        return "storyteller/form";
    }

    @PostMapping("/tales/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TaleForm form,
                         BindingResult result,
                         Model model,
                         Principal principal,
                         HttpSession session) {
        Tale tale = editableTale(id, principal);
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("tale", tale);
            return "storyteller/form";
        }
        try {
            applyForm(tale, form, false, session);
            // any edit goes back through review
            tale.setStatus(TaleStatus.PENDING);
            tale.setReviewNote(null);
            tale.setApprovedAt(null);
            taleService.save(tale);
        } catch (IllegalArgumentException e) {
            result.reject("storage", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("tale", tale);
            return "storyteller/form";
        }
        return "redirect:/storyteller/tales?updated";
    }

    private void applyForm(Tale tale, TaleForm form, boolean audioRequired, HttpSession session) {
        tale.setTitle(form.getTitle().trim());
        tale.setDescription(form.getDescription().trim());

        Set<Category> categories = form.getCategoryIds().stream()
                .map(categoryService::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        tale.setCategories(categories);

        boolean appliedAudio = false;
        String preUploaded = form.getAudioFilename() == null ? "" : form.getAudioFilename().trim();
        if (!preUploaded.isEmpty()) {
            Set<String> pending = StorytellerUploadApiController.pendingSet(session);
            if (!pending.contains(preUploaded)) {
                throw new IllegalArgumentException("فایل صوتی آپلودشده یافت نشد؛ لطفاً دوباره آپلود کنید");
            }
            Resource resource = storage.load(FileStorageService.AUDIO, preUploaded);
            if (resource == null) {
                throw new IllegalArgumentException("فایل صوتی آپلودشده یافت نشد؛ لطفاً دوباره آپلود کنید");
            }
            if (tale.getAudioPath() != null && !tale.getAudioPath().equals(preUploaded)) {
                storage.delete(FileStorageService.AUDIO, tale.getAudioPath());
            }
            tale.setAudioPath(preUploaded);
            String contentType = form.getAudioContentType();
            tale.setAudioContentType(contentType == null || contentType.isBlank() ? "audio/mpeg" : contentType);
            tale.setDurationSeconds(null);
            pending.remove(preUploaded);
            appliedAudio = true;
        } else if (form.getAudio() != null && !form.getAudio().isEmpty()) {
            String newAudio = storage.storeAudio(form.getAudio());
            if (tale.getAudioPath() != null) {
                storage.delete(FileStorageService.AUDIO, tale.getAudioPath());
            }
            tale.setAudioPath(newAudio);
            tale.setAudioContentType(form.getAudio().getContentType());
            tale.setDurationSeconds(null);
            appliedAudio = true;
        }

        if (audioRequired && !appliedAudio) {
            throw new IllegalArgumentException("لطفاً صدای قصه را انتخاب یا ضبط کنید");
        }

        if (form.getCover() != null && !form.getCover().isEmpty()) {
            String newCover = storage.storeCover(form.getCover());
            if (tale.getCoverPath() != null) {
                storage.delete(FileStorageService.COVERS, tale.getCoverPath());
            }
            tale.setCoverPath(newCover);
        }
    }

    private Tale editableTale(Long id, Principal principal) {
        Tale tale = taleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        User user = currentUser(principal);
        boolean owner = tale.getStoryteller().getId().equals(user.getId());
        if (!owner || tale.getStatus() == TaleStatus.APPROVED) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return tale;
    }

    private User currentUser(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
