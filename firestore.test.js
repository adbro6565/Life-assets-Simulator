const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write user profiles, holdings, or synced assets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("holdings").doc("crypto_btc").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("assets").doc("crypto_01").get()
  );
});

test("Authenticated user: can create and read their own profile, holding, and synced asset with image & online price metadata", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const pastTime = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_billionaire",
      displayName: "Alice",
      cashBalance: 500000.0,
      netWorth: 500000.0,
      streakDays: 1,
      lastClaimDateKathmandu: "2026-10-20",
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("holdings").doc("crypto_btc").set({
      userId: ALICE_UID,
      assetId: "crypto_btc",
      symbol: "BTC",
      name: "Bitcoin",
      category: "CRYPTOCURRENCIES",
      quantity: 2.5,
      averageCostBasis: 68000.0,
      imageUrl: "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?w=900",
      imageSource: "Unsplash Editorial CDN",
      licenseInfo: "Unsplash License",
      verificationStatus: "VERIFIED_REAL_PHOTO",
      onlinePriceUsd: 88450.0,
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assets").doc("art_01").set({
      userId: ALICE_UID,
      assetId: "art_01",
      symbol: "MONA",
      name: "Mona Lisa — Leonardo da Vinci",
      category: "FINE_ART",
      imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/800px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg",
      thumbnailUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/320px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg",
      imageSource: "Wikimedia Commons • Musée du Louvre Official Reproduction",
      licenseInfo: "Public Domain (PD-Art / C2RMF High-Resolution Archival Scan)",
      verificationStatus: "VERIFIED_REAL_PHOTO",
      onlinePriceUsd: 860000000.0,
      priceSource: "French Ministry of Culture / Musée du Louvre",
      priceType: "INSURANCE_VALUATION",
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("holdings").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assets").get()
  );
});

test("Authenticated user: cannot read or modify another user's holdings, synced assets, or profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const pastTime = new Date(Date.now() - 2000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("holdings").doc("art_mona_lisa").set({
      userId: BOB_UID,
      assetId: "art_mona_lisa",
      symbol: "MONA",
      name: "Mona Lisa",
      category: "FINE_ART",
      quantity: 1,
      averageCostBasis: 860000000.0,
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("assets").doc("crypto_01").set({
      userId: BOB_UID,
      assetId: "crypto_01",
      symbol: "BTC",
      name: "Bitcoin",
      category: "CRYPTOCURRENCIES",
      imageUrl: "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?w=900",
      imageSource: "Unsplash Editorial CDN",
      licenseInfo: "Unsplash License",
      verificationStatus: "VERIFIED_REAL_PHOTO",
      onlinePriceUsd: 88450.0,
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("holdings").doc("art_mona_lisa").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("holdings").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("assets").doc("crypto_01").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("assets").get()
  );
});

test("Shadow update test: rejects ghost fields on holding and syncedAsset creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const pastTime = new Date(Date.now() - 2000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("holdings").doc("crypto_eth").set({
      userId: ALICE_UID,
      assetId: "crypto_eth",
      symbol: "ETH",
      name: "Ethereum",
      category: "CRYPTOCURRENCIES",
      quantity: 10,
      averageCostBasis: 3400.0,
      isAdmin: true,
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("assets").doc("crypto_02").set({
      userId: ALICE_UID,
      assetId: "crypto_02",
      symbol: "ETH",
      name: "Ethereum",
      category: "CRYPTOCURRENCIES",
      imageUrl: "https://images.unsplash.com/photo-1621761191319-c6fb62004040?w=900",
      imageSource: "Unsplash Editorial CDN",
      licenseInfo: "Unsplash License",
      verificationStatus: "VERIFIED_REAL_PHOTO",
      onlinePriceUsd: 3420.0,
      ghostAdminFlag: true,
      createdAt: pastTime,
      updatedAt: pastTime,
    })
  );
});
