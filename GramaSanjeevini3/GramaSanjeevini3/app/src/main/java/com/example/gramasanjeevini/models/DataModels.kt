package com.example.gramasanjeevini.models

import kotlin.math.*

data class Medicine(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val uses: String = "",
    val sideEffects: String = ""
)

data class Shop(
    val id: String = "",
    val name: String = "",
    val village: String = "",
    val district: String = "",
    val distanceKm: Double = 0.0,
    val phone: String = "",
    val ownerName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isRegisteredByPharmacist: Boolean = false
)

data class StockItem(
    val id: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val medicineCategory: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val shopVillage: String = "",
    val shopDistrict: String = "",
    val shopLat: Double = 0.0,
    val shopLng: Double = 0.0,
    val distanceKm: Double = 0.0,
    val quantity: Int = 0,
    val expiryDate: String = "",
    val isNearExpiry: Boolean = false,
    val discountPercent: Int = 0
)

// ── Haversine distance ────────────────────────────────────────────────────────
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

// ── 12 shops across 4 Karnataka districts ────────────────────────────────────
val MOCK_SHOPS = listOf(
    Shop("shop1",  "Sri Rama Medicals",      "Hulkoti",      "Gadag",              3.2,  "9845001111", "Ramesh Kumar",   15.4167, 75.6167),
    Shop("shop2",  "Dhanvantari Pharmacy",   "Gadag",        "Gadag",              7.5,  "9845002222", "Suresh Patil",   15.4167, 75.6333),
    Shop("shop3",  "Arogya Drug Store",      "Shirhatti",    "Gadag",             12.0,  "9845003333", "Mahesh Naik",    15.2333, 75.5833),
    Shop("shop4",  "Hemavathi Medicals",     "Hassan",       "Hassan",           220.0,  "9845004444", "Girish Gowda",   13.0068, 76.1004),
    Shop("shop5",  "Belur Pharmacy",         "Belur",        "Hassan",           195.0,  "9845005555", "Ravi Shetty",    13.1667, 75.8667),
    Shop("shop6",  "Sakleshpur Drug House",  "Sakleshpur",   "Hassan",           210.0,  "9845006666", "Anand Kumar",    12.9333, 75.7833),
    Shop("shop7",  "Mangala Medicals",       "Mangalore",    "Dakshina Kannada", 310.0,  "9845007777", "Deepak Shetty",  12.8698, 74.8431),
    Shop("shop8",  "Coastal Pharmacy",       "Udupi",        "Udupi",            280.0,  "9845008888", "Harish Prabhu",  13.3409, 74.7421),
    Shop("shop9",  "Bantwal Drug Store",     "Bantwal",      "Dakshina Kannada", 330.0,  "9845009999", "Sunil Rai",      12.8917, 75.0333),
    Shop("shop10", "Rajajinagar Medicals",   "Rajajinagar",  "Bangalore",        410.0,  "9845010000", "Venkat Reddy",   12.9915, 77.5530),
    Shop("shop11", "Whitefield Pharmacy",    "Whitefield",   "Bangalore",        430.0,  "9845011111", "Priya Sharma",   12.9698, 77.7500),
    Shop("shop12", "Yelahanka Drug House",   "Yelahanka",    "Bangalore",        400.0,  "9845012222", "Kiran Naik",     13.1007, 77.5963)
)

// ── 100 Medicines catalogue ───────────────────────────────────────────────────
val MOCK_MEDICINES = listOf(
    // Life Saving (15)
    Medicine("m1",  "Insulin (Regular)",         "Life Saving", "Diabetes management",              "Controls blood sugar in Type-1 & Type-2 diabetes",    "Hypoglycemia if overdosed"),
    Medicine("m2",  "Snake Antivenom",            "Life Saving", "Emergency snake bite treatment",   "Neutralises venom toxins",                            "Allergic reaction possible"),
    Medicine("m3",  "Adrenaline (Epi-Pen)",       "Life Saving", "Anaphylaxis / severe allergy",     "Reverses life-threatening allergic reactions",        "Rapid heart rate"),
    Medicine("m4",  "Atropine Injection",         "Life Saving", "Organophosphate poisoning / bradycardia", "Blocks nerve agent effects, raises heart rate","Dry mouth, blurred vision"),
    Medicine("m5",  "Morphine Injection",         "Life Saving", "Severe acute pain / MI",           "Powerful opioid pain relief",                         "Respiratory depression"),
    Medicine("m6",  "Hydrocortisone Injection",   "Life Saving", "Adrenal crisis / severe asthma",   "Rapid anti-inflammatory steroid",                     "Hyperglycemia"),
    Medicine("m7",  "Diazepam Injection",         "Life Saving", "Status epilepticus / seizures",    "Stops prolonged seizures",                            "Sedation, respiratory depression"),
    Medicine("m8",  "Oxytocin Injection",         "Life Saving", "Postpartum haemorrhage",           "Contracts uterus to stop bleeding after delivery",    "Hypotension"),
    Medicine("m9",  "Magnesium Sulphate Inj",     "Life Saving", "Eclampsia / pre-eclampsia",        "Prevents seizures in pregnancy",                      "Flushing, respiratory depression"),
    Medicine("m10", "Naloxone Injection",         "Life Saving", "Opioid overdose reversal",         "Rapidly reverses opioid toxicity",                    "Withdrawal symptoms"),
    Medicine("m11", "Activated Charcoal",         "Life Saving", "Poisoning / drug overdose",        "Adsorbs toxins in the gut",                           "Black stools, constipation"),
    Medicine("m12", "Scorpion Antivenom",         "Life Saving", "Scorpion sting emergency",         "Neutralises scorpion venom",                          "Allergic reaction possible"),
    Medicine("m13", "Rabies Immunoglobulin",      "Life Saving", "Post-exposure rabies prophylaxis", "Provides immediate passive immunity",                 "Local pain, fever"),
    Medicine("m14", "Tetanus Toxoid Injection",   "Life Saving", "Tetanus prevention after wound",   "Stimulates tetanus antibody production",              "Local soreness"),
    Medicine("m15", "Amlodipine 5mg",             "Life Saving", "Hypertensive emergency",           "Calcium channel blocker, lowers BP rapidly",          "Ankle swelling, flushing"),

    // Essential (45)
    Medicine("m16", "ORS Packets",                "Essential",   "Oral rehydration salts",           "Prevents dehydration in diarrhoea & vomiting",        "None significant"),
    Medicine("m17", "Paracetamol 500mg",          "Essential",   "Fever & mild pain relief",         "Reduces fever and pain",                              "Liver damage if overdosed"),
    Medicine("m18", "Amoxicillin 500mg",          "Essential",   "Broad-spectrum antibiotic",        "Treats bacterial infections",                         "Nausea, diarrhoea"),
    Medicine("m19", "Metformin 500mg",            "Essential",   "Type-2 diabetes tablet",           "Lowers blood glucose",                                "Stomach upset"),
    Medicine("m20", "Omeprazole 20mg",            "Essential",   "Acidity & ulcer relief",           "Reduces stomach acid",                                "Headache, nausea"),
    Medicine("m21", "Azithromycin 500mg",         "Essential",   "Antibiotic for respiratory infections", "Treats pneumonia, typhoid, STIs",               "Nausea, diarrhoea"),
    Medicine("m22", "Ciprofloxacin 500mg",        "Essential",   "Broad-spectrum antibiotic",        "Treats UTI, typhoid, diarrhoea",                      "Tendon damage, nausea"),
    Medicine("m23", "Doxycycline 100mg",          "Essential",   "Antibiotic / anti-malarial",       "Treats malaria, leptospirosis, chlamydia",            "Photosensitivity, nausea"),
    Medicine("m24", "Chloroquine 250mg",          "Essential",   "Anti-malarial",                    "Treats and prevents malaria",                         "Nausea, headache"),
    Medicine("m25", "Artemether-Lumefantrine",    "Essential",   "Anti-malarial (ACT)",              "First-line treatment for P. falciparum malaria",      "Dizziness, headache"),
    Medicine("m26", "Cotrimoxazole 480mg",        "Essential",   "Antibiotic / anti-infective",      "Treats UTI, pneumocystis pneumonia",                  "Rash, nausea"),
    Medicine("m27", "Metronidazole 400mg",        "Essential",   "Antibiotic / antiprotozoal",       "Treats amoebic dysentery, giardia, anaerobic infections", "Metallic taste, nausea"),
    Medicine("m28", "Albendazole 400mg",          "Essential",   "Anthelmintic / deworming",         "Kills intestinal worms",                              "Nausea, abdominal pain"),
    Medicine("m29", "Ivermectin 12mg",            "Essential",   "Antiparasitic",                    "Treats scabies, filariasis, strongyloidiasis",        "Dizziness, rash"),
    Medicine("m30", "Ibuprofen 400mg",            "Essential",   "Anti-inflammatory / pain relief",  "Reduces pain, fever, inflammation",                   "Gastric irritation, kidney risk"),
    Medicine("m31", "Diclofenac 50mg",            "Essential",   "NSAID / pain relief",              "Treats joint pain, muscle pain, dysmenorrhoea",       "Gastric ulcer, kidney risk"),
    Medicine("m32", "Tramadol 50mg",              "Essential",   "Moderate-severe pain",             "Opioid-like analgesic",                               "Nausea, dizziness, dependence"),
    Medicine("m33", "Prednisolone 5mg",           "Essential",   "Corticosteroid / anti-inflammatory","Treats asthma, allergies, autoimmune conditions",    "Weight gain, hyperglycemia"),
    Medicine("m34", "Salbutamol Inhaler",         "Essential",   "Bronchodilator / asthma",          "Relieves acute bronchospasm",                         "Tremor, palpitations"),
    Medicine("m35", "Budesonide Inhaler",         "Essential",   "Inhaled steroid / asthma",         "Prevents asthma attacks",                             "Oral thrush, hoarseness"),
    Medicine("m36", "Theophylline 200mg",         "Essential",   "Bronchodilator / COPD",            "Relaxes airway muscles",                              "Nausea, arrhythmia"),
    Medicine("m37", "Enalapril 5mg",              "Essential",   "ACE inhibitor / hypertension",     "Lowers blood pressure, protects kidneys",             "Dry cough, hypotension"),
    Medicine("m38", "Losartan 50mg",              "Essential",   "ARB / hypertension",               "Lowers blood pressure",                               "Dizziness, hyperkalemia"),
    Medicine("m39", "Furosemide 40mg",            "Essential",   "Loop diuretic / oedema",           "Removes excess fluid in heart failure, oedema",       "Electrolyte imbalance"),
    Medicine("m40", "Spironolactone 25mg",        "Essential",   "Potassium-sparing diuretic",       "Treats heart failure, ascites",                       "Hyperkalemia, gynecomastia"),
    Medicine("m41", "Digoxin 0.25mg",             "Essential",   "Cardiac glycoside / heart failure","Strengthens heart contractions, controls AF",         "Toxicity: nausea, arrhythmia"),
    Medicine("m42", "Aspirin 75mg",               "Essential",   "Antiplatelet / cardiac protection","Prevents heart attack and stroke",                    "Gastric bleeding"),
    Medicine("m43", "Clopidogrel 75mg",           "Essential",   "Antiplatelet",                     "Prevents blood clots after MI / stent",               "Bleeding risk"),
    Medicine("m44", "Atorvastatin 10mg",          "Essential",   "Statin / cholesterol",             "Lowers LDL cholesterol",                              "Muscle pain, liver enzyme rise"),
    Medicine("m45", "Glibenclamide 5mg",          "Essential",   "Sulphonylurea / diabetes",         "Stimulates insulin release",                          "Hypoglycemia"),
    Medicine("m46", "Levothyroxine 50mcg",        "Essential",   "Thyroid hormone replacement",      "Treats hypothyroidism",                               "Palpitations if overdosed"),
    Medicine("m47", "Carbimazole 5mg",            "Essential",   "Antithyroid",                      "Treats hyperthyroidism",                              "Agranulocytosis, rash"),
    Medicine("m48", "Ferrous Sulphate 200mg",     "Essential",   "Iron supplement / anaemia",        "Treats iron-deficiency anaemia",                      "Constipation, dark stools"),
    Medicine("m49", "Folic Acid 5mg",             "Essential",   "Vitamin B9 supplement",            "Prevents neural tube defects, treats anaemia",        "None significant"),
    Medicine("m50", "Vitamin B12 500mcg",         "Essential",   "Cyanocobalamin supplement",        "Treats B12 deficiency, neuropathy",                   "None significant"),
    Medicine("m51", "Calcium + Vitamin D3",       "Essential",   "Bone health supplement",           "Prevents osteoporosis, rickets",                      "Constipation, hypercalcemia"),
    Medicine("m52", "Zinc Sulphate 20mg",         "Essential",   "Micronutrient / diarrhoea",        "Reduces diarrhoea duration in children",              "Nausea"),
    Medicine("m53", "Vitamin A 200000 IU",        "Essential",   "Vitamin A supplement",             "Prevents night blindness, boosts immunity",           "Toxicity if overdosed"),
    Medicine("m54", "Oral Contraceptive Pill",    "Essential",   "Hormonal contraception",           "Prevents pregnancy",                                  "Nausea, mood changes"),
    Medicine("m55", "Misoprostol 200mcg",         "Essential",   "Uterotonic / ulcer",               "Prevents PPH, treats gastric ulcer",                  "Cramping, diarrhoea"),
    Medicine("m56", "Fluconazole 150mg",          "Essential",   "Antifungal",                       "Treats vaginal candidiasis, oral thrush",              "Nausea, headache"),
    Medicine("m57", "Acyclovir 400mg",            "Essential",   "Antiviral / herpes",               "Treats herpes simplex, chickenpox, shingles",         "Nausea, headache"),
    Medicine("m58", "Oseltamivir 75mg",           "Essential",   "Antiviral / influenza",            "Treats and prevents influenza A & B",                 "Nausea, vomiting"),
    Medicine("m59", "Rifampicin 450mg",           "Essential",   "Anti-TB antibiotic",               "First-line tuberculosis treatment",                   "Orange urine, hepatotoxicity"),
    Medicine("m60", "Isoniazid 300mg",            "Essential",   "Anti-TB antibiotic",               "First-line tuberculosis treatment",                   "Peripheral neuropathy, hepatitis"),

    // General (40)
    Medicine("m61", "Atenolol 50mg",              "General",     "Beta-blocker / hypertension",      "Reduces heart rate & BP",                             "Dizziness, fatigue"),
    Medicine("m62", "Cetirizine 10mg",            "General",     "Antihistamine / allergy",          "Relieves sneezing, itching, runny nose",              "Drowsiness"),
    Medicine("m63", "Loratadine 10mg",            "General",     "Non-drowsy antihistamine",         "Relieves allergy symptoms without sedation",          "Headache, dry mouth"),
    Medicine("m64", "Levocetirizine 5mg",         "General",     "Antihistamine / allergy",          "Relieves allergic rhinitis, urticaria",               "Mild drowsiness"),
    Medicine("m65", "Montelukast 10mg",           "General",     "Leukotriene antagonist / asthma",  "Prevents asthma and allergic rhinitis",               "Headache, mood changes"),
    Medicine("m66", "Pantoprazole 40mg",          "General",     "Proton pump inhibitor",            "Treats GERD, peptic ulcer",                           "Headache, diarrhoea"),
    Medicine("m67", "Ranitidine 150mg",           "General",     "H2 blocker / acidity",             "Reduces stomach acid",                                "Headache, constipation"),
    Medicine("m68", "Domperidone 10mg",           "General",     "Antiemetic / nausea",              "Relieves nausea, vomiting, bloating",                 "Dry mouth, headache"),
    Medicine("m69", "Ondansetron 4mg",            "General",     "Antiemetic / vomiting",            "Prevents chemotherapy and post-op nausea",            "Headache, constipation"),
    Medicine("m70", "Loperamide 2mg",             "General",     "Antidiarrhoeal",                   "Reduces frequency of loose stools",                   "Constipation, abdominal cramps"),
    Medicine("m71", "Bisacodyl 5mg",              "General",     "Laxative / constipation",          "Stimulates bowel movement",                           "Abdominal cramps"),
    Medicine("m72", "Lactulose Syrup",            "General",     "Osmotic laxative",                 "Treats constipation and hepatic encephalopathy",      "Bloating, flatulence"),
    Medicine("m73", "Antacid (Aluminium Hydroxide)","General",   "Antacid / acidity",                "Neutralises stomach acid",                            "Constipation"),
    Medicine("m74", "Dicyclomine 10mg",           "General",     "Antispasmodic / IBS",              "Relieves intestinal cramps and spasms",               "Dry mouth, blurred vision"),
    Medicine("m75", "Mebeverine 135mg",           "General",     "Antispasmodic / IBS",              "Relieves IBS-related abdominal pain",                 "Nausea, dizziness"),
    Medicine("m76", "Clonazepam 0.5mg",           "General",     "Benzodiazepine / anxiety",         "Treats anxiety, panic disorder, epilepsy",            "Sedation, dependence"),
    Medicine("m77", "Sertraline 50mg",            "General",     "SSRI antidepressant",              "Treats depression, OCD, PTSD",                        "Nausea, insomnia, sexual dysfunction"),
    Medicine("m78", "Amitriptyline 25mg",         "General",     "Tricyclic antidepressant",         "Treats depression, neuropathic pain, migraine",       "Dry mouth, sedation"),
    Medicine("m79", "Haloperidol 5mg",            "General",     "Antipsychotic",                    "Treats schizophrenia, acute psychosis",               "EPS, sedation"),
    Medicine("m80", "Phenobarbitone 60mg",        "General",     "Anticonvulsant / epilepsy",        "Controls generalised seizures",                       "Sedation, dependence"),
    Medicine("m81", "Carbamazepine 200mg",        "General",     "Anticonvulsant / epilepsy",        "Treats epilepsy, trigeminal neuralgia",               "Dizziness, rash"),
    Medicine("m82", "Phenytoin 100mg",            "General",     "Anticonvulsant / epilepsy",        "Controls tonic-clonic seizures",                      "Gum hyperplasia, ataxia"),
    Medicine("m83", "Gabapentin 300mg",           "General",     "Anticonvulsant / neuropathic pain","Treats nerve pain, epilepsy",                         "Dizziness, somnolence"),
    Medicine("m84", "Pregabalin 75mg",            "General",     "Neuropathic pain / anxiety",       "Treats diabetic neuropathy, fibromyalgia",            "Dizziness, weight gain"),
    Medicine("m85", "Metoclopramide 10mg",        "General",     "Antiemetic / gastroparesis",       "Relieves nausea, speeds gastric emptying",            "EPS, drowsiness"),
    Medicine("m86", "Promethazine 25mg",          "General",     "Antihistamine / antiemetic",       "Treats motion sickness, nausea, allergy",             "Sedation, dry mouth"),
    Medicine("m87", "Chlorpheniramine 4mg",       "General",     "Antihistamine / cold",             "Relieves cold, allergy, hay fever",                   "Drowsiness, dry mouth"),
    Medicine("m88", "Betahistine 16mg",           "General",     "Vestibular / vertigo",             "Treats Meniere's disease and vertigo",                "Nausea, headache"),
    Medicine("m89", "Cinnarizine 25mg",           "General",     "Antivertigo / motion sickness",    "Treats vertigo, motion sickness",                     "Drowsiness, weight gain"),
    Medicine("m90", "Dextromethorphan Syrup",     "General",     "Cough suppressant",                "Suppresses dry cough",                                "Dizziness, drowsiness"),
    Medicine("m91", "Bromhexine 8mg",             "General",     "Expectorant / cough",              "Loosens mucus in chest",                              "Nausea, dizziness"),
    Medicine("m92", "Ambroxol 30mg",              "General",     "Mucolytic / cough",                "Thins and clears mucus from airways",                 "Nausea, gastric discomfort"),
    Medicine("m93", "Clotrimazole Cream",         "General",     "Antifungal cream",                 "Treats ringworm, athlete's foot, candidiasis",        "Local irritation"),
    Medicine("m94", "Betamethasone Cream",        "General",     "Topical steroid",                  "Treats eczema, psoriasis, dermatitis",                "Skin thinning if overused"),
    Medicine("m95", "Calamine Lotion",            "General",     "Antipruritic / skin",              "Relieves itching from rash, chickenpox, insect bites","None significant"),
    Medicine("m96", "Povidone Iodine Solution",   "General",     "Antiseptic / wound care",          "Disinfects wounds and skin",                          "Iodine allergy possible"),
    Medicine("m97", "Gentamicin Eye Drops",       "General",     "Antibiotic eye drops",             "Treats bacterial conjunctivitis",                     "Local stinging"),
    Medicine("m98", "Chloramphenicol Eye Drops",  "General",     "Antibiotic eye drops",             "Treats eye infections",                               "Local irritation"),
    Medicine("m99", "Ear Drops (Ciprofloxacin)",  "General",     "Antibiotic ear drops",             "Treats otitis externa, ear infections",               "Local irritation"),
    Medicine("m100","Multivitamin Tablet",        "General",     "General nutritional supplement",   "Fills micronutrient gaps",                            "None significant")
)

// ── Symptom → medicine mapping (150+ symptom keywords) ───────────────────────
val SYMPTOM_MAP = mapOf(
    // Fever / Pain
    "fever"            to listOf("Paracetamol 500mg", "Ibuprofen 400mg"),
    "temperature"      to listOf("Paracetamol 500mg"),
    "high fever"       to listOf("Paracetamol 500mg", "Ibuprofen 400mg"),
    "headache"         to listOf("Paracetamol 500mg", "Ibuprofen 400mg"),
    "migraine"         to listOf("Ibuprofen 400mg", "Amitriptyline 25mg"),
    "body pain"        to listOf("Paracetamol 500mg", "Ibuprofen 400mg", "Diclofenac 50mg"),
    "pain"             to listOf("Paracetamol 500mg", "Ibuprofen 400mg"),
    "joint pain"       to listOf("Diclofenac 50mg", "Ibuprofen 400mg"),
    "muscle pain"      to listOf("Diclofenac 50mg", "Ibuprofen 400mg"),
    "back pain"        to listOf("Diclofenac 50mg", "Ibuprofen 400mg"),
    "knee pain"        to listOf("Diclofenac 50mg", "Ibuprofen 400mg"),
    "arthritis"        to listOf("Diclofenac 50mg", "Ibuprofen 400mg", "Prednisolone 5mg"),
    "nerve pain"       to listOf("Gabapentin 300mg", "Pregabalin 75mg", "Amitriptyline 25mg"),
    "neuropathy"       to listOf("Gabapentin 300mg", "Pregabalin 75mg", "Vitamin B12 500mcg"),
    "severe pain"      to listOf("Tramadol 50mg", "Morphine Injection"),

    // Cold / Cough / Respiratory
    "cold"             to listOf("Paracetamol 500mg", "Cetirizine 10mg", "Chlorpheniramine 4mg"),
    "cough"            to listOf("Dextromethorphan Syrup", "Bromhexine 8mg", "Ambroxol 30mg"),
    "dry cough"        to listOf("Dextromethorphan Syrup"),
    "wet cough"        to listOf("Ambroxol 30mg", "Bromhexine 8mg"),
    "productive cough" to listOf("Ambroxol 30mg", "Bromhexine 8mg"),
    "chest congestion" to listOf("Ambroxol 30mg", "Salbutamol Inhaler"),
    "flu"              to listOf("Paracetamol 500mg", "Oseltamivir 75mg"),
    "influenza"        to listOf("Oseltamivir 75mg", "Paracetamol 500mg"),
    "chills"           to listOf("Paracetamol 500mg"),
    "runny nose"       to listOf("Cetirizine 10mg", "Chlorpheniramine 4mg", "Loratadine 10mg"),
    "blocked nose"     to listOf("Cetirizine 10mg", "Chlorpheniramine 4mg"),
    "sneezing"         to listOf("Cetirizine 10mg", "Loratadine 10mg"),
    "sore throat"      to listOf("Amoxicillin 500mg", "Paracetamol 500mg"),
    "throat pain"      to listOf("Amoxicillin 500mg", "Paracetamol 500mg"),
    "tonsil"           to listOf("Amoxicillin 500mg", "Azithromycin 500mg"),
    "tonsillitis"      to listOf("Amoxicillin 500mg", "Azithromycin 500mg"),
    "pneumonia"        to listOf("Amoxicillin 500mg", "Azithromycin 500mg", "Ciprofloxacin 500mg"),
    "bronchitis"       to listOf("Amoxicillin 500mg", "Salbutamol Inhaler", "Ambroxol 30mg"),

    // Asthma / Breathing
    "asthma"           to listOf("Salbutamol Inhaler", "Budesonide Inhaler", "Montelukast 10mg"),
    "breathlessness"   to listOf("Salbutamol Inhaler", "Budesonide Inhaler"),
    "wheezing"         to listOf("Salbutamol Inhaler", "Theophylline 200mg"),
    "shortness of breath" to listOf("Salbutamol Inhaler", "Furosemide 40mg"),
    "copd"             to listOf("Salbutamol Inhaler", "Theophylline 200mg", "Budesonide Inhaler"),

    // Malaria / Tropical
    "malaria"          to listOf("Artemether-Lumefantrine", "Chloroquine 250mg", "Doxycycline 100mg"),
    "typhoid"          to listOf("Ciprofloxacin 500mg", "Azithromycin 500mg", "Paracetamol 500mg"),
    "dengue"           to listOf("Paracetamol 500mg", "ORS Packets"),
    "chikungunya"      to listOf("Paracetamol 500mg", "Ibuprofen 400mg"),
    "leptospirosis"    to listOf("Doxycycline 100mg", "Amoxicillin 500mg"),
    "filariasis"       to listOf("Ivermectin 12mg", "Albendazole 400mg"),
    "worms"            to listOf("Albendazole 400mg", "Ivermectin 12mg"),
    "intestinal worms" to listOf("Albendazole 400mg"),
    "deworming"        to listOf("Albendazole 400mg"),
    "scabies"          to listOf("Ivermectin 12mg", "Calamine Lotion"),

    // Snake / Scorpion / Poisoning
    "snake"            to listOf("Snake Antivenom"),
    "snake bite"       to listOf("Snake Antivenom"),
    "venom"            to listOf("Snake Antivenom", "Scorpion Antivenom"),
    "bite"             to listOf("Snake Antivenom"),
    "scorpion"         to listOf("Scorpion Antivenom"),
    "scorpion sting"   to listOf("Scorpion Antivenom"),
    "poisoning"        to listOf("Activated Charcoal", "Atropine Injection"),
    "overdose"         to listOf("Activated Charcoal", "Naloxone Injection"),
    "organophosphate"  to listOf("Atropine Injection"),
    "pesticide"        to listOf("Atropine Injection", "Activated Charcoal"),
    "rabies"           to listOf("Rabies Immunoglobulin"),
    "dog bite"         to listOf("Rabies Immunoglobulin", "Tetanus Toxoid Injection"),
    "animal bite"      to listOf("Rabies Immunoglobulin", "Tetanus Toxoid Injection"),
    "tetanus"          to listOf("Tetanus Toxoid Injection"),
    "wound"            to listOf("Tetanus Toxoid Injection", "Povidone Iodine Solution", "Amoxicillin 500mg"),
    "cut"              to listOf("Povidone Iodine Solution", "Tetanus Toxoid Injection"),
    "injury"           to listOf("Povidone Iodine Solution", "Ibuprofen 400mg"),

    // Allergy / Skin
    "allergy"          to listOf("Cetirizine 10mg", "Loratadine 10mg", "Adrenaline (Epi-Pen)"),
    "allergic"         to listOf("Cetirizine 10mg", "Levocetirizine 5mg"),
    "itching"          to listOf("Cetirizine 10mg", "Calamine Lotion", "Betamethasone Cream"),
    "rash"             to listOf("Cetirizine 10mg", "Calamine Lotion", "Betamethasone Cream"),
    "hives"            to listOf("Cetirizine 10mg", "Levocetirizine 5mg"),
    "urticaria"        to listOf("Cetirizine 10mg", "Levocetirizine 5mg", "Prednisolone 5mg"),
    "eczema"           to listOf("Betamethasone Cream", "Cetirizine 10mg"),
    "psoriasis"        to listOf("Betamethasone Cream", "Prednisolone 5mg"),
    "dermatitis"       to listOf("Betamethasone Cream", "Calamine Lotion"),
    "ringworm"         to listOf("Clotrimazole Cream", "Fluconazole 150mg"),
    "fungal infection" to listOf("Clotrimazole Cream", "Fluconazole 150mg"),
    "athlete's foot"   to listOf("Clotrimazole Cream"),
    "chickenpox"       to listOf("Acyclovir 400mg", "Calamine Lotion", "Cetirizine 10mg"),
    "herpes"           to listOf("Acyclovir 400mg"),
    "shingles"         to listOf("Acyclovir 400mg", "Gabapentin 300mg"),
    "anaphylaxis"      to listOf("Adrenaline (Epi-Pen)", "Hydrocortisone Injection"),
    "severe allergy"   to listOf("Adrenaline (Epi-Pen)"),
    "swelling"         to listOf("Cetirizine 10mg", "Adrenaline (Epi-Pen)", "Furosemide 40mg"),

    // Stomach / GI
    "dehydration"      to listOf("ORS Packets"),
    "diarrhoea"        to listOf("ORS Packets", "Loperamide 2mg", "Zinc Sulphate 20mg"),
    "diarrhea"         to listOf("ORS Packets", "Loperamide 2mg"),
    "loose motion"     to listOf("ORS Packets", "Loperamide 2mg"),
    "vomiting"         to listOf("ORS Packets", "Ondansetron 4mg", "Domperidone 10mg"),
    "nausea"           to listOf("Domperidone 10mg", "Ondansetron 4mg", "Metoclopramide 10mg"),
    "motion sickness"  to listOf("Promethazine 25mg", "Cinnarizine 25mg"),
    "acidity"          to listOf("Omeprazole 20mg", "Pantoprazole 40mg", "Antacid (Aluminium Hydroxide)"),
    "acid reflux"      to listOf("Omeprazole 20mg", "Pantoprazole 40mg"),
    "gerd"             to listOf("Pantoprazole 40mg", "Omeprazole 20mg"),
    "heartburn"        to listOf("Omeprazole 20mg", "Antacid (Aluminium Hydroxide)"),
    "ulcer"            to listOf("Omeprazole 20mg", "Pantoprazole 40mg", "Misoprostol 200mcg"),
    "stomach pain"     to listOf("Omeprazole 20mg", "Dicyclomine 10mg", "ORS Packets"),
    "stomach ache"     to listOf("Dicyclomine 10mg", "Omeprazole 20mg"),
    "stomach cramps"   to listOf("Dicyclomine 10mg", "Mebeverine 135mg"),
    "abdominal pain"   to listOf("Dicyclomine 10mg", "Mebeverine 135mg"),
    "ibs"              to listOf("Mebeverine 135mg", "Dicyclomine 10mg"),
    "bloating"         to listOf("Domperidone 10mg", "Antacid (Aluminium Hydroxide)"),
    "gas"              to listOf("Antacid (Aluminium Hydroxide)", "Domperidone 10mg"),
    "indigestion"      to listOf("Omeprazole 20mg", "Antacid (Aluminium Hydroxide)"),
    "constipation"     to listOf("Bisacodyl 5mg", "Lactulose Syrup"),
    "gastric"          to listOf("Omeprazole 20mg", "Pantoprazole 40mg"),
    "amoeba"           to listOf("Metronidazole 400mg"),
    "amoebic dysentery"  to listOf("Metronidazole 400mg", "Ciprofloxacin 500mg"),
    "giardia"          to listOf("Metronidazole 400mg"),
    "ors"              to listOf("ORS Packets"),

    // Diabetes
    "diabetes"         to listOf("Insulin (Regular)", "Metformin 500mg", "Glibenclamide 5mg"),
    "sugar"            to listOf("Insulin (Regular)", "Metformin 500mg"),
    "blood sugar"      to listOf("Insulin (Regular)", "Metformin 500mg", "Glibenclamide 5mg"),
    "high sugar"       to listOf("Insulin (Regular)", "Metformin 500mg"),
    "insulin"          to listOf("Insulin (Regular)"),
    "glucose"          to listOf("Insulin (Regular)", "Metformin 500mg"),
    "type 2 diabetes"  to listOf("Metformin 500mg", "Glibenclamide 5mg"),

    // Heart / BP
    "blood pressure"   to listOf("Atenolol 50mg", "Amlodipine 5mg", "Enalapril 5mg"),
    "bp"               to listOf("Atenolol 50mg", "Losartan 50mg"),
    "hypertension"     to listOf("Atenolol 50mg", "Amlodipine 5mg", "Losartan 50mg"),
    "high bp"          to listOf("Atenolol 50mg", "Amlodipine 5mg"),
    "heart rate"       to listOf("Atenolol 50mg", "Digoxin 0.25mg"),
    "palpitation"      to listOf("Atenolol 50mg"),
    "heart failure"    to listOf("Furosemide 40mg", "Digoxin 0.25mg", "Spironolactone 25mg"),
    "oedema"           to listOf("Furosemide 40mg", "Spironolactone 25mg"),
    "swollen legs"     to listOf("Furosemide 40mg"),
    "cholesterol"      to listOf("Atorvastatin 10mg"),
    "high cholesterol" to listOf("Atorvastatin 10mg"),
    "heart attack"     to listOf("Aspirin 75mg", "Clopidogrel 75mg"),
    "stroke"           to listOf("Aspirin 75mg", "Clopidogrel 75mg"),
    "chest pain"       to listOf("Aspirin 75mg", "Morphine Injection"),

    // Thyroid
    "thyroid"          to listOf("Levothyroxine 50mcg", "Carbimazole 5mg"),
    "hypothyroid"      to listOf("Levothyroxine 50mcg"),
    "hyperthyroid"     to listOf("Carbimazole 5mg"),
    "goitre"           to listOf("Levothyroxine 50mcg"),

    // Anaemia / Nutrition
    "anaemia"          to listOf("Ferrous Sulphate 200mg", "Folic Acid 5mg", "Vitamin B12 500mcg"),
    "anemia"           to listOf("Ferrous Sulphate 200mg", "Folic Acid 5mg"),
    "weakness"         to listOf("Ferrous Sulphate 200mg", "Multivitamin Tablet", "Vitamin B12 500mcg"),
    "fatigue"          to listOf("Ferrous Sulphate 200mg", "Multivitamin Tablet"),
    "tiredness"        to listOf("Multivitamin Tablet", "Ferrous Sulphate 200mg"),
    "vitamin deficiency" to listOf("Multivitamin Tablet", "Vitamin B12 500mcg", "Vitamin A 200000 IU"),
    "night blindness"  to listOf("Vitamin A 200000 IU"),
    "rickets"          to listOf("Calcium + Vitamin D3"),
    "bone pain"        to listOf("Calcium + Vitamin D3", "Ibuprofen 400mg"),
    "osteoporosis"     to listOf("Calcium + Vitamin D3"),

    // Infection / Antibiotic
    "infection"        to listOf("Amoxicillin 500mg", "Ciprofloxacin 500mg"),
    "bacterial"        to listOf("Amoxicillin 500mg", "Azithromycin 500mg"),
    "uti"              to listOf("Ciprofloxacin 500mg", "Cotrimoxazole 480mg"),
    "urinary infection"  to listOf("Ciprofloxacin 500mg", "Cotrimoxazole 480mg"),
    "ear infection"    to listOf("Amoxicillin 500mg", "Ear Drops (Ciprofloxacin)"),
    "ear pain"         to listOf("Ear Drops (Ciprofloxacin)", "Paracetamol 500mg"),
    "pus"              to listOf("Amoxicillin 500mg", "Ciprofloxacin 500mg"),
    "abscess"          to listOf("Amoxicillin 500mg", "Ciprofloxacin 500mg"),
    "tuberculosis"     to listOf("Rifampicin 450mg", "Isoniazid 300mg"),
    "tb"               to listOf("Rifampicin 450mg", "Isoniazid 300mg"),

    // Eye
    "eye infection"    to listOf("Gentamicin Eye Drops", "Chloramphenicol Eye Drops"),
    "conjunctivitis"   to listOf("Gentamicin Eye Drops", "Chloramphenicol Eye Drops"),
    "red eye"          to listOf("Gentamicin Eye Drops"),
    "eye pain"         to listOf("Chloramphenicol Eye Drops", "Paracetamol 500mg"),

    // Epilepsy / Neuro
    "seizure"          to listOf("Diazepam Injection", "Phenobarbitone 60mg", "Carbamazepine 200mg"),
    "epilepsy"         to listOf("Carbamazepine 200mg", "Phenytoin 100mg", "Phenobarbitone 60mg"),
    "fits"             to listOf("Diazepam Injection", "Carbamazepine 200mg"),
    "convulsion"       to listOf("Diazepam Injection", "Phenobarbitone 60mg"),
    "vertigo"          to listOf("Betahistine 16mg", "Cinnarizine 25mg"),
    "dizziness"        to listOf("Betahistine 16mg", "Cinnarizine 25mg"),
    "meniere"          to listOf("Betahistine 16mg"),

    // Mental Health
    "anxiety"          to listOf("Clonazepam 0.5mg", "Sertraline 50mg"),
    "depression"       to listOf("Sertraline 50mg", "Amitriptyline 25mg"),
    "insomnia"         to listOf("Clonazepam 0.5mg", "Promethazine 25mg"),
    "sleep"            to listOf("Clonazepam 0.5mg"),
    "panic"            to listOf("Clonazepam 0.5mg", "Sertraline 50mg"),
    "psychosis"        to listOf("Haloperidol 5mg"),
    "schizophrenia"    to listOf("Haloperidol 5mg"),

    // Women's Health
    "pregnancy"        to listOf("Folic Acid 5mg", "Ferrous Sulphate 200mg"),
    "contraception"    to listOf("Oral Contraceptive Pill"),
    "period pain"      to listOf("Ibuprofen 400mg", "Diclofenac 50mg"),
    "dysmenorrhoea"    to listOf("Ibuprofen 400mg", "Diclofenac 50mg"),
    "bleeding"         to listOf("Oxytocin Injection", "Misoprostol 200mcg"),
    "postpartum"       to listOf("Oxytocin Injection", "Misoprostol 200mcg"),
    "eclampsia"        to listOf("Magnesium Sulphate Inj"),
    "candidiasis"      to listOf("Fluconazole 150mg", "Clotrimazole Cream"),
    "vaginal discharge"  to listOf("Fluconazole 150mg", "Metronidazole 400mg")
)

// ── Mock stock: one entry per medicine per shop (subset for demo) ─────────────
val MOCK_STOCK: List<StockItem> = run {
    // Assign a representative subset of medicines to each shop
    val shopMedicines = mapOf(
        "shop1"  to listOf("m17","m16","m18","m30","m62","m20","m66","m70","m28","m17","m42","m44","m48","m90","m91"),
        "shop2"  to listOf("m17","m21","m22","m25","m34","m37","m19","m62","m63","m66","m68","m71","m87","m93","m96"),
        "shop3"  to listOf("m17","m16","m18","m24","m30","m62","m20","m70","m28","m48","m49","m52","m90","m95","m96"),
        "shop4"  to listOf("m17","m16","m18","m21","m22","m30","m34","m37","m44","m62","m66","m68","m71","m87","m93"),
        "shop5"  to listOf("m17","m16","m25","m30","m34","m37","m44","m48","m62","m63","m66","m70","m90","m91","m96"),
        "shop6"  to listOf("m17","m16","m18","m22","m30","m62","m20","m66","m68","m70","m87","m90","m91","m93","m95"),
        "shop7"  to listOf("m1","m3","m5","m6","m7","m8","m9","m10","m11","m15","m17","m21","m25","m34","m37"),
        "shop8"  to listOf("m2","m12","m13","m14","m17","m16","m18","m21","m22","m30","m34","m44","m62","m66","m96"),
        "shop9"  to listOf("m17","m16","m18","m21","m25","m30","m34","m37","m44","m48","m62","m66","m70","m90","m93"),
        "shop10" to listOf("m1","m4","m5","m6","m7","m15","m17","m19","m21","m22","m25","m34","m37","m44","m57"),
        "shop11" to listOf("m17","m16","m18","m21","m22","m30","m34","m37","m44","m48","m56","m62","m66","m68","m93"),
        "shop12" to listOf("m17","m16","m18","m21","m25","m30","m34","m37","m44","m62","m66","m70","m87","m90","m96")
    )

    val stockList = mutableListOf<StockItem>()
    MOCK_SHOPS.forEach { shop ->
        val medIds = shopMedicines[shop.id] ?: emptyList()
        medIds.forEachIndexed { idx, medId ->
            val med = MOCK_MEDICINES.find { it.id == medId } ?: return@forEachIndexed
            stockList.add(
                StockItem(
                    id               = "${shop.id}_${med.id}",
                    medicineId       = med.id,
                    medicineName     = med.name,
                    medicineCategory = med.category,
                    shopId           = shop.id,
                    shopName         = shop.name,
                    shopVillage      = shop.village,
                    shopDistrict     = shop.district,
                    shopLat          = shop.lat,
                    shopLng          = shop.lng,
                    distanceKm       = shop.distanceKm,
                    quantity         = 10 + (idx * 7) % 90,
                    expiryDate       = "2026-${(idx % 12 + 1).toString().padStart(2,'0')}-15",
                    isNearExpiry     = idx % 10 == 0,
                    discountPercent  = if (idx % 10 == 0) 20 else 0
                )
            )
        }
    }
    stockList
}
