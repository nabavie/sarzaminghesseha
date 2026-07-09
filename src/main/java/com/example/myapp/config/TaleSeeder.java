package com.example.myapp.config;

import com.example.myapp.model.Category;
import com.example.myapp.model.Role;
import com.example.myapp.model.Tale;
import com.example.myapp.model.TaleStatus;
import com.example.myapp.model.User;
import com.example.myapp.repository.CategoryRepository;
import com.example.myapp.repository.TaleRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

/**
 * Seeds a set of classic Persian folk tales (public-domain folklore, narrated
 * with a Persian TTS voice) so the app has real, playable content out of the box.
 * Audio lives in classpath:seed/audio and is copied into the uploads dir on
 * first boot; each tale is only created if no tale with the same title exists.
 */
@Component
@Order(2) // after DataSeeder: needs the seeded categories
public class TaleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TaleSeeder.class);

    private static final String STORYTELLER_USERNAME = "qessegoo";

    private record SeedTale(String file, String title, String description,
                            int durationSeconds, List<String> categories) {
    }

    private static final List<SeedTale> TALES = List.of(
            new SeedTale("kadu-qelqelezan.mp3", "کدو قلقله‌زن",
                    "پیرزنی برای دیدن دخترش از کوه می‌گذرد و گرگ و پلنگ و شیر سر راهش سبز می‌شوند. "
                            + "برگشتنی، با یک کدوی قلقله‌زن و کمی زیرکی از دست همه‌شان در می‌رود! "
                            + "یکی از شیرین‌ترین قصه‌های عامیانهٔ ایرانی برای بچه‌ها.",
                    147, List.of("قصه‌های محلی", "حیوانات", "پندآموز")),
            new SeedTale("shangul-mangul.mp3", "شنگول و منگول",
                    "بزی سه بزغاله دارد: شنگول، منگول و حبهٔ انگور. گرگ ناقلا با پای آردشده در می‌زند... "
                            + "قصهٔ معروف بزِ زنگوله‌پا که به بچه‌ها یاد می‌دهد در را به روی غریبه باز نکنند.",
                    147, List.of("قصه‌های محلی", "حیوانات")),
            new SeedTale("mah-pishooni.mp3", "ماه‌پیشونی",
                    "دخترکی مهربان که با نامادری‌اش زندگی می‌کند، دنبال پنبه‌ای که آب برده به خانه‌ای جادویی می‌رسد. "
                            + "مهربانی‌اش ماهی درخشان بر پیشانی‌اش می‌نشاند. سیندرلای ایرانی!",
                    141, List.of("افسانه", "پندآموز")),
            new SeedTale("nokhodi.mp3", "نخودی",
                    "پسرکی به اندازهٔ یک نخود، اما با دلی بزرگ و فکری زرنگ، آش پدرش را می‌برد و "
                            + "سر راه، دیوی را که راه مردم را بسته بود فراری می‌دهد. کوچک بودن مهم نیست!",
                    120, List.of("قصه‌های محلی", "ماجراجویی")),
            new SeedTale("amoo-norooz.mp3", "عمو نوروز و ننه سرما",
                    "هر سال روز اول بهار، عمو نوروز از پشت کوه‌ها می‌آید و ننه سرما که چشم‌به‌راهش نشسته، "
                            + "درست همان لحظه خوابش می‌برد... افسانهٔ قشنگ نوروز و بهار برای شب عید.",
                    128, List.of("افسانه", "قصه‌های محلی")),
            new SeedTale("hasan-kachal.mp3", "حسن کچل",
                    "حسن تنبل‌ترین آدم آبادی است و از کنار تنور تکان نمی‌خورد؛ تا اینکه یک شب گرسنه می‌ماند و "
                            + "می‌فهمد نان در بازوی آدم است. قصه‌ای شیرین دربارهٔ کار و تلاش.",
                    124, List.of("قصه‌های محلی", "پندآموز")),
            new SeedTale("khale-sooske.mp3", "خاله سوسکه",
                    "خاله سوسکه چادر گل‌گلی‌اش را سر می‌کند و راه می‌افتد تا دوستی مهربان پیدا کند. "
                            + "به هرکس می‌رسد یک سؤال دارد: «اگر از دستم دلخور شوی، چه می‌کنی؟» قصه‌ای دربارهٔ دوستِ خوب.",
                    120, List.of("حیوانات", "پندآموز")),
            new SeedTale("tooti-bazargan.mp3", "طوطی و بازرگان",
                    "بازرگانی عازم هندوستان از طوطی‌اش می‌پرسد چه سوغاتی می‌خواهد و طوطی فقط یک پیغام دارد... "
                            + "حکایت دل‌نشین مثنوی مولوی دربارهٔ آزادی و دوست داشتن، بازگفته برای بچه‌ها.",
                    145, List.of("پندآموز", "افسانه")));

    private final TaleRepository taleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService storage;
    private final PasswordEncoder passwordEncoder;
    private final String storytellerPassword;

    public TaleSeeder(TaleRepository taleRepository,
                      UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      FileStorageService storage,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.storyteller-password:Qessegoo1234}") String storytellerPassword) {
        this.taleRepository = taleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.storage = storage;
        this.passwordEncoder = passwordEncoder;
        this.storytellerPassword = storytellerPassword;
    }

    @Override
    public void run(String... args) {
        User storyteller = userRepository.findByUsername(STORYTELLER_USERNAME)
                .orElseGet(this::createStoryteller);

        for (SeedTale seed : TALES) {
            if (taleExists(seed.title())) {
                continue;
            }
            ClassPathResource resource = new ClassPathResource("seed/audio/" + seed.file());
            if (!resource.exists()) {
                log.warn("Seed audio missing on classpath: {}", seed.file());
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                String audioPath = storage.storeStream(in, FileStorageService.AUDIO, "mp3");
                Tale tale = new Tale();
                tale.setTitle(seed.title());
                tale.setDescription(seed.description());
                tale.setStoryteller(storyteller);
                tale.setAudioPath(audioPath);
                tale.setAudioContentType("audio/mpeg");
                tale.setDurationSeconds(seed.durationSeconds());
                tale.setStatus(TaleStatus.APPROVED);
                tale.setApprovedAt(Instant.now());
                for (String name : seed.categories()) {
                    categoryRepository.findByName(name).ifPresent(tale.getCategories()::add);
                }
                taleRepository.save(tale);
                log.info("Seeded tale '{}'", seed.title());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not seed tale audio " + seed.file(), e);
            }
        }
    }

    private boolean taleExists(String title) {
        return taleRepository.findAll().stream().anyMatch(t -> title.equals(t.getTitle()));
    }

    private User createStoryteller() {
        User user = new User(STORYTELLER_USERNAME,
                passwordEncoder.encode(storytellerPassword),
                "قصه‌گوی سرزمین قصه‌ها");
        user.getRoles().add(Role.LISTENER);
        user.getRoles().add(Role.STORYTELLER);
        log.info("Seeded storyteller user '{}'", STORYTELLER_USERNAME);
        return userRepository.save(user);
    }
}
