package com.example.myapp.controller;

import com.example.myapp.seo.FaqItem;
import com.example.myapp.seo.StructuredData;
import com.example.myapp.util.SiteUrl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SitePagesController {

    private static final List<FaqItem> FAQS = List.of(
            new FaqItem(
                    "سرزمین قصه‌ها چیست؟",
                    "سرزمین قصه‌ها یک وب‌سایت فارسی برای شنیدن قصه صوتی و قصه شب است. "
                            + "اینجا قصه‌گویان داستان‌های کودکانه، نوجوانانه و سنتی را با صدای خود ضبط می‌کنند "
                            + "تا خانواده‌ها بتوانند هر زمان قصه‌ای آرام و دلنشین گوش بدهند."
            ),
            new FaqItem(
                    "هدف این وب‌سایت چیست؟",
                    "هدف ما زنده نگه داشتن فرهنگ قصه‌گویی، ساختن آرشیوی از قصه‌های صوتی باکیفیت، "
                            + "و فراهم کردن فضایی امن و دوستانه برای کودکان، نوجوانان و والدین است. "
                            + "می‌خواهیم پلی باشیم میان قصه‌های کهن ایران و شنوندگان امروز."
            ),
            new FaqItem(
                    "این سایت برای چه کسانی است؟",
                    "برای کودکان و نوجوانان، والدین، معلمان، و همهٔ کسانی که عاشق شنیدن یا گفتن قصه هستند. "
                            + "هم می‌توانید شنونده باشید و هم در صورت تأیید، به‌عنوان قصه‌گو قصه منتشر کنید."
            ),
            new FaqItem(
                    "چطور قصه گوش بدهم؟",
                    "از صفحهٔ اصلی یا بخش «همهٔ قصه‌ها» قصهٔ موردنظر را باز کنید و روی پخش‌کنندهٔ صوتی بزنید. "
                            + "مهمان‌ها می‌توانند تعداد محدودی قصه را رایگان بشنوند؛ با ثبت‌نام، به قصه‌های بیشتری دسترسی دارید "
                            + "و پیشرفت شنیدن‌تان ذخیره می‌شود."
            ),
            new FaqItem(
                    "آیا استفاده از سایت رایگان است؟",
                    "بله، شنیدن قصه‌های منتشرشده برای کاربران رایگان است. "
                            + "مهمان‌ها محدودیت تعداد شنیدن دارند و با ساخت حساب کاربری، تجربهٔ کامل‌تری خواهید داشت."
            ),
            new FaqItem(
                    "چطور قصه‌گو شوم و قصه منتشر کنم؟",
                    "پس از ثبت‌نام، می‌توانید نقش قصه‌گو را فعال کنید (یا از مدیریت بخواهید). "
                            + "سپس قصهٔ صوتی خود را بارگذاری کنید. هر قصه پس از بررسی و تأیید مدیر منتشر می‌شود."
            ),
            new FaqItem(
                    "چرا بعضی قصه‌ها هنوز دیده نمی‌شوند؟",
                    "هر قصهٔ جدید ابتدا توسط تیم مدیریت بررسی می‌شود تا محتوا مناسب و باکیفیت باشد. "
                            + "پس از تأیید، قصه در فهرست عمومی و نقشهٔ سایت قرار می‌گیرد."
            ),
            new FaqItem(
                    "چطور نظر یا پیشنهاد بدهم؟",
                    "از صفحهٔ «نظر و پیشنهاد» پیام خود را برای ما بفرستید. "
                            + "ایده‌ها و بازخورد شما به بهتر شدن سرزمین قصه‌ها کمک می‌کند."
            )
    );

    private final SiteUrl siteUrl;

    public SitePagesController(SiteUrl siteUrl) {
        this.siteUrl = siteUrl;
    }

    @GetMapping("/about")
    public String about(Model model, HttpServletRequest request) {
        String base = siteUrl.base(request);
        model.addAttribute("pageDescription",
                "دربارهٔ سرزمین قصه‌ها — وب‌سایت قصه صوتی و قصه شب برای کودکان و نوجوانان. "
                        + "با هدف زنده نگه داشتن قصه‌گویی و ساختن آرشیوی از داستان‌های صوتی فارسی.");
        model.addAttribute("pageCanonical", base + "/about");
        model.addAttribute("jsonLd", StructuredData.aboutPage(base));
        return "pages/about";
    }

    @GetMapping("/faq")
    public String faq(Model model, HttpServletRequest request) {
        String base = siteUrl.base(request);
        model.addAttribute("faqs", FAQS);
        model.addAttribute("pageDescription",
                "سؤالات متداول دربارهٔ سرزمین قصه‌ها: قصه صوتی چیست، هدف سایت چیست، "
                        + "چطور گوش بدهیم و چطور قصه‌گو شویم.");
        model.addAttribute("pageCanonical", base + "/faq");
        model.addAttribute("jsonLd", StructuredData.faqPage(base, FAQS));
        return "pages/faq";
    }
}
