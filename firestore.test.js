const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const assert = require("node:assert");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("Default deny blocks unauthorized collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("secret_vault").doc("doc1").get());
});

test("Allows reading and creating members with valid schema", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const validMember = {
    memberId: 101,
    memberCode: "TTS-1447-101",
    fullName: "Hazrat Maulana",
    designation: "सदर (President)",
    committeeWing: "मुख्य प्रबंधक बोर्ड",
    phoneNumber: "9876543210"
  };
  await assertSucceeds(db.collection("members").doc("101").set(validMember));
  await assertSucceeds(db.collection("members").doc("101").get());
});

test("Rejects member creation with invalid or missing required fields", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const invalidMember = {
    fullName: "A" // too short and missing designation, committeeWing, phoneNumber
  };
  await assertFails(db.collection("members").doc("102").set(invalidMember));
});

test("Allows reading and writing donations with valid schema", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const donation = {
    donationId: 201,
    donorName: "Mohammad Ali",
    amount: 5100,
    purpose: "12 रबी-उल-अव्वल लंगर"
  };
  await assertSucceeds(db.collection("donations").doc("201").set(donation));
  await assertSucceeds(db.collection("donations").doc("201").get());
});

test("Allows creating and reading live chat messages", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const msg = {
    messageId: 301,
    channelId: "general",
    senderName: "Khadim",
    messageText: "अस्सलामु अलैकुम, बैठक शुरू हो गई है।"
  };
  await assertSucceeds(db.collection("chat_messages").doc("301").set(msg));
  await assertSucceeds(db.collection("chat_messages").doc("301").get());
});

test("Allows creating and reading notices", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const notice = {
    noticeId: 401,
    title: "जुलूस-ए-मोहम्मदी का समय",
    content: "सभी पदाधिकारी सुबह 8 बजे उपस्थित हों।",
    issuedBy: "कमेटी सदर"
  };
  await assertSucceeds(db.collection("notices").doc("401").set(notice));
  await assertSucceeds(db.collection("notices").doc("401").get());
});
