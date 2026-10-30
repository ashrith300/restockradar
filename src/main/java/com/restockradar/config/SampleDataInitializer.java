package com.restockradar.config;

import com.restockradar.model.*;
import com.restockradar.repository.*;
import com.restockradar.service.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SampleDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final StockIntakeRepository stockIntakeRepository;
    private final InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;

    public SampleDataInitializer(UserRepository userRepository,
                                 CategoryRepository categoryRepository,
                                 SupplierRepository supplierRepository,
                                 ProductRepository productRepository,
                                 SaleRepository saleRepository,
                                 StockIntakeRepository stockIntakeRepository,
                                 InventoryService inventoryService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.stockIntakeRepository = stockIntakeRepository;
        this.inventoryService = inventoryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Create or update user: ashrithrao28@gmail.com (Sathvik Pharmacy)
        User user = userRepository.findByEmail("ashrithrao28@gmail.com").orElse(null);
        if (user == null) {
            user = new User();
            user.setOwnerName("Ashrith Rao");
            user.setShopName("sathvik pharmacy");
            user.setEmail("ashrithrao28@gmail.com");
            user.setPhone("+91-9876543210");
            user.setPassword(passwordEncoder.encode("ashrith6965"));
            user = userRepository.save(user);
        } else {
            user.setShopName("sathvik pharmacy");
            user.setOwnerName("Ashrith Rao");
            user.setPassword(passwordEncoder.encode("ashrith6965"));
            user = userRepository.save(user);
        }

        // Also ensure demo retailer user exists for testing
        if (userRepository.findByEmail("retailer@shop.com").isEmpty()) {
            User demo = new User();
            demo.setOwnerName("Demo Owner");
            demo.setShopName("Demo Pharmacy");
            demo.setEmail("retailer@shop.com");
            demo.setPhone("+91-9123456789");
            demo.setPassword(passwordEncoder.encode("password"));
            userRepository.save(demo);
        }

        // 2. Create Realistic Pharmacy Categories
        String[] catNames = {
                "Pain Relief & Antipyretics",
                "Antibiotics & Anti-Infectives",
                "Diabetes & Cardiac Care",
                "Gastrointestinal & Antacids",
                "Respiratory, Cough & Cold",
                "Dermatology & Skin Care",
                "Vitamins & Nutritional Supplements",
                "First Aid & Surgical Essentials",
                "Diagnostics & Health Monitors",
                "Ayurvedic & Herbal Wellness"
        };
        String[] catDescs = {
                "Analgesics, fever reducers, anti-inflammatory medicines and body pain remedies",
                "Prescription broad-spectrum antibiotics, antibacterial creams and tablets",
                "Blood sugar controllers, insulin accessories, hypertension and cholesterol regulators",
                "Proton-pump inhibitors, antacid suspensions, digestive enzymes and laxatives",
                "Expectorant syrups, decongestants, inhalers and anti-allergy antihistamines",
                "Antifungal ointments, antiseptic creams, burn dressings and medicated lotions",
                "Multivitamins, Zinc, Vitamin C, Calcium + D3 and immunity boosters",
                "Sterile gauze, cotton rolls, elastic bandages, band-aids and surgical tapes",
                "Glucometers, blood glucose test strips, digital thermometers and pulse oximeters",
                "Herbal health restoratives, digestives, liver tonics and natural balms"
        };

        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < catNames.length; i++) {
            final String name = catNames[i];
            Category cat = categoryRepository.findAll().stream()
                    .filter(c -> c.getCategoryName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (cat == null) {
                cat = new Category(name, catDescs[i]);
                cat = categoryRepository.save(cat);
            }
            categories.add(cat);
        }

        // 3. Create Realistic Pharma Distributors / Suppliers with real phone numbers for WhatsApp
        String[] supplierNames = {
                "Sun Pharma Wholesale Depot",
                "Cipla Healthcare Distributors",
                "Apollo Pharma Logistics",
                "Dr. Reddy's Direct Supply",
                "Mankind Health Supplies",
                "Zydus Life Distributors",
                "Abbott Healthcare Network",
                "Glenmark Pharma Trade",
                "Dettol & Reckitt Commercial",
                "Himalaya Wellness Depot"
        };
        String[] contacts = {
                "Dr. Suresh Reddy", "K. Venkat Rao", "Priya Sharma", "Ramesh Gupta",
                "Anil Kumar", "Vikram Singh", "Neha Verma", "Rajesh Patel",
                "Amitabh Sen", "Mahesh Bhatt"
        };
        String[] phones = {
                "+919849012345", "+919876543211", "+919988776655", "+919701234567",
                "+919811223344", "+919900112233", "+919822334455", "+919833445566",
                "+919844556677", "+919855667788"
        };

        List<Supplier> suppliers = new ArrayList<>();
        for (int i = 0; i < supplierNames.length; i++) {
            final String name = supplierNames[i];
            Supplier sup = supplierRepository.findAll().stream()
                    .filter(s -> s.getSupplierName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (sup == null) {
                sup = new Supplier();
                sup.setSupplierName(name);
                sup.setContactName(contacts[i]);
                sup.setEmail("orders@" + name.toLowerCase().replaceAll("[^a-z]", "") + ".com");
                sup.setPhone(phones[i]);
                sup.setAddress("Pharma Hub Sector " + (i + 1) + ", Hyderabad / Bengaluru, India");
                sup = supplierRepository.save(sup);
            }
            suppliers.add(sup);
        }

        // 4. Seed Products for ashrithrao28@gmail.com (Sathvik Pharmacy) if not already seeded
        if (productRepository.countByUser(user) == 0) {
            seedPharmacyInventoryAndSales(user, categories, suppliers);
        }
    }

    private void seedPharmacyInventoryAndSales(User user, List<Category> categories, List<Supplier> suppliers) {
        // Array of realistic pharmacy items:
        // { CategoryIdx, SupplierIdx, Name, CostPrice, SellingPrice, Stock, MinStock, ExpiryDaysFromNow, BatchNumber }
        Object[][] pharmaData = {
                // Pain Relief
                {0, 0, "Dolo 650mg Tablets (15s)", 22.00, 34.00, 45, 20, 420, "DL-2026-881"},
                {0, 1, "Crocin Advance 500mg (15s)", 15.00, 24.50, 8, 15, 380, "CR-2026-412"}, // Low stock
                {0, 2, "Combiflam Pain Relief (20s)", 32.00, 48.00, 30, 10, 500, "CB-2026-902"},
                {0, 4, "Volini Pain Relief Gel 50g", 85.00, 135.00, 12, 10, 290, "VL-2026-118"},
                {0, 7, "Moov Pain Relief Spray 80g", 120.00, 180.00, 5, 8, 310, "MV-2026-641"}, // Low stock

                // Antibiotics
                {1, 0, "Augmentin 625 Duo Tablets (10s)", 140.00, 205.00, 6, 12, 18, "AG-2026-044"}, // Near Expiry (18 days) & Low stock
                {1, 1, "Azithral 500mg Tablets (5s)", 82.00, 122.00, 25, 10, 450, "AZ-2026-773"},
                {1, 2, "Ciproglen 500mg Tablets (10s)", 35.00, 55.00, 4, 10, 390, "CP-2026-301"}, // Low stock
                {1, 5, "Taxim-O 200mg Tablets (10s)", 78.00, 115.00, 18, 8, 360, "TX-2026-820"},

                // Diabetes & Cardiac Care
                {2, 3, "Glycomet 500mg SR Tablets (20s)", 30.00, 48.00, 60, 25, 600, "GL-2026-551"},
                {2, 3, "Telma 40mg Cardiac Tablets (30s)", 135.00, 210.00, 9, 15, 520, "TL-2026-104"}, // Low stock
                {2, 5, "Atorva 20mg Cholesterol (15s)", 160.00, 245.00, 22, 10, 480, "AT-2026-993"},
                {2, 6, "Thyronorm 50mcg Tablets (100s)", 125.00, 195.00, 14, 10, 365, "TH-2026-215"},

                // Gastrointestinal & Antacids
                {3, 0, "Pan 40mg Gastro Tablets (15s)", 95.00, 150.00, 50, 15, 410, "PN-2026-440"},
                {3, 3, "Omez 20mg Capsules (20s)", 42.00, 68.00, 35, 12, 340, "OM-2026-889"},
                {3, 6, "Gelusil MPS Antacid Liquid 200ml", 78.00, 120.00, 3, 10, 12, "GL-2026-031"}, // Low stock & Expiring soon (12 days)
                {3, 7, "Pantocid DSR Capsules (15s)", 145.00, 230.00, 28, 12, 490, "PC-2026-712"},
                {3, 9, "Pudinhara Pearls Digest (10s)", 18.00, 30.00, 40, 15, 300, "PD-2026-112"},

                // Respiratory & Cough
                {4, 1, "Ascoril LS Cough Syrup 100ml", 72.00, 118.00, 16, 12, 40, "AS-2026-029"}, // Expiring in 40 days
                {4, 1, "Cheston Cold Tablets (10s)", 32.00, 50.00, 42, 15, 450, "CH-2026-664"},
                {4, 6, "Allegra 120mg Allergy (10s)", 125.00, 198.00, 5, 10, 520, "AL-2026-339"}, // Low stock
                {4, 6, "Benadryl Cough Formula 100ml", 68.00, 110.00, 4, 10, -5, "BN-2025-901"}, // Expired 5 days ago (showcase alert!)
                {4, 8, "Vicks VapoRub Balm 50g", 65.00, 95.00, 35, 10, 700, "VK-2026-455"},
                {4, 4, "Cofsils Lozenges Honey (10s)", 20.00, 35.00, 80, 20, 420, "CF-2026-102"},

                // Dermatology & Skin Care
                {5, 2, "Betadine 10% Antiseptic 100ml", 85.00, 130.00, 20, 8, 380, "BT-2026-512"},
                {5, 6, "Soframycin Skin Cream 30g", 42.00, 65.00, 25, 10, 400, "SF-2026-281"},
                {5, 5, "Candid B Anti-Fungal Cream 20g", 75.00, 118.00, 12, 8, 340, "CD-2026-778"},

                // Vitamins & Supplements
                {6, 0, "Becosules Z Capsules (30s)", 35.00, 54.00, 70, 20, 510, "BC-2026-302"},
                {6, 6, "Limcee 500mg Chewable (15s)", 18.00, 28.50, 90, 25, 440, "LM-2026-619"},
                {6, 7, "Shelcal 500mg Tablets (15s)", 75.00, 120.00, 32, 15, 600, "SH-2026-880"},
                {6, 0, "Neurobion Forte Tablets (30s)", 28.00, 42.00, 65, 20, 480, "NB-2026-114"},
                {6, 6, "Supradyn Daily Multivitamin (15s)", 38.00, 58.00, 40, 15, 530, "SP-2026-921"},

                // First Aid & Essentials
                {7, 4, "Electral ORS Sachet 21.8g", 14.00, 22.00, 110, 30, 620, "EL-2026-401"},
                {7, 8, "Hansaplast Washproof (20s)", 35.00, 55.00, 45, 15, 800, "HP-2026-701"},
                {7, 8, "Dettol Antiseptic Liquid 250ml", 95.00, 145.00, 28, 10, 750, "DT-2026-331"},
                {7, 2, "Cotton Absorbent Roll 100g", 30.00, 50.00, 22, 8, 900, "CT-2026-224"},

                // Diagnostics
                {8, 6, "Digital Thermometer Flexi", 140.00, 220.00, 15, 5, 1200, "TH-2026-559"},
                {8, 6, "Accu-Chek Active 50 Strips", 680.00, 950.00, 7, 10, 320, "AC-2026-098"}, // Low stock

                // Ayurvedic
                {9, 9, "Liv.52 DS Tablets (60s)", 125.00, 185.00, 35, 12, 600, "LV-2026-447"}
        };

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < pharmaData.length; i++) {
            Product p = new Product();
            p.setUser(user);
            p.setCategory(categories.get((int) pharmaData[i][0]));
            p.setSupplier(suppliers.get((int) pharmaData[i][1]));
            p.setProductName((String) pharmaData[i][2]);
            p.setCostPrice(BigDecimal.valueOf(((Number) pharmaData[i][3]).doubleValue()));
            p.setSellingPrice(BigDecimal.valueOf(((Number) pharmaData[i][4]).doubleValue()));
            p.setCurrentStock((int) pharmaData[i][5]);
            p.setMinimumStock((int) pharmaData[i][6]);
            p.setExpiryDate(LocalDate.now().plusDays((int) pharmaData[i][7]));
            p.setBatchNumber((String) pharmaData[i][8]);
            p.setBarcode("8901089" + String.format("%05d", i + 1));
            p = productRepository.save(p);
            products.add(p);
        }

        // 5. Generate 1 Full Year (365 Days) of 300+ Realistic Sales Records
        Random rand = new Random(42); // deterministic seed for reproducibility
        int totalSalesCreated = 0;

        for (int day = 365; day >= 0; day--) {
            LocalDateTime baseDay = LocalDateTime.now().minusDays(day);

            // Small shops typically have 1 to 4 sales per day across their catalog
            // More sales on weekend days (Sat/Sun)
            boolean isWeekend = baseDay.getDayOfWeek().getValue() >= 6;
            int salesToday = isWeekend ? (rand.nextInt(3) + 2) : (rand.nextInt(2) + 1);

            for (int s = 0; s < salesToday; s++) {
                Product p = products.get(rand.nextInt(products.size()));
                int quantity = rand.nextInt(3) + 1; // 1 to 3 packs

                LocalDateTime saleTimestamp = baseDay
                        .withHour(9 + rand.nextInt(11)) // 9am - 8pm
                        .withMinute(rand.nextInt(59))
                        .withSecond(rand.nextInt(59));

                Sale sale = new Sale();
                sale.setProduct(p);
                sale.setQuantitySold(quantity);
                sale.setSellingPrice(p.getSellingPrice());
                sale.setTotalAmount(p.getSellingPrice().multiply(BigDecimal.valueOf(quantity)));
                sale.setSaleDate(saleTimestamp);
                saleRepository.save(sale);
                totalSalesCreated++;
            }
        }

        // 6. Generate initial Stock Intake history (to demonstrate the new feature)
        for (int i = 0; i < 8; i++) {
            Product p = products.get(i * 4 % products.size());
            StockIntake intake = new StockIntake();
            intake.setProduct(p);
            intake.setSupplier(p.getSupplier());
            int intakeQty = (rand.nextInt(5) + 2) * 10;
            intake.setQuantityReceived(intakeQty);
            intake.setPurchaseUnitPrice(p.getCostPrice());
            intake.setTotalCost(p.getCostPrice().multiply(BigDecimal.valueOf(intakeQty)));
            intake.setBatchNumber(p.getBatchNumber());
            intake.setExpiryDate(p.getExpiryDate());
            intake.setInvoiceNumber("INV-2026-" + (1001 + i));
            intake.setNotes("Regular monthly stock shipment received and verified.");
            intake.setReceivedAt(LocalDateTime.now().minusDays(i * 12 + 3));
            stockIntakeRepository.save(intake);
        }

        // 7. Recalculate inventory status, recommendations, and reports for Sathvik Pharmacy
        for (Product p : products) {
            inventoryService.recalcInventory(p);
            inventoryService.generateRecommendation(p);
        }
        inventoryService.generateReports(products);

        System.out.println("==========================================================");
        System.out.println(" Successfully seeded Sathvik Pharmacy for ashrithrao28@gmail.com");
        System.out.println(" Generated " + products.size() + " medicines and " + totalSalesCreated + " sales over 365 days!");
        System.out.println("==========================================================");
    }
}
