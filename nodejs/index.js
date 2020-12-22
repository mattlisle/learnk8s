const path = require('path');
const express = require('express');
const minio = require('minio');
const multer = require('multer');
const marked = require('marked');
const MongoClient = require('mongodb').MongoClient;

const minioHost = process.env.MINIO_HOST || 'localhost';
const minioBucket = 'image-storage';
const mongoURL = process.env.MONGO_URL || 'mongodb://localhost:27017/dev'
const app = express();
const port = process.env.PORT || 3000;

async function initMinIO() {
  console.log('Initialising MinIO...');
  const client = new minio.Client({
    endPoint: minioHost,
    port: 9000,
    useSSL: false,
    accessKey: process.env.MINIO_ACCESS_KEY,
    secretKey: process.env.MINIO_SECRET_KEY,
  });
  let success = false;
  while (!success) {
    try {
      if (!(await client.bucketExists(minioBucket))) {
        await client.makeBucket(minioBucket);
      }
      success = true;
    } catch {
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }
  console.log('MinIO initialised');
  return client;
}

async function initMongo() {
  console.log('Initialising MongoDB...');
  let success = false;
  while (!success) {
    try {
      client = await MongoClient.connect(mongoURL, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
      });
      success = true;
    } catch {
      console.log('Error connecting to MongoDB, retrying in 1 second');
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }
  console.log('MongoDB initialised');
  return client.db(client.s.options.dbName).collection('notes');
}

async function retrieveNotes(db) {
  const notes = (await db.find().toArray()).reverse();
  return notes.map(it => {
    return { ...it, description: marked(it.description) };
  });
}

async function saveNote(db, note) {
  await db.insertOne(note)
}

async function start() {
  const db = await initMongo();
  const minioClient = await initMinIO();
  app.set('view engine', 'pug');
  app.set('views', path.join(__dirname, 'views'));
  app.use(express.static(path.join(__dirname, 'public')));

  app.get('/', async (req, res) => {
    res.render('index', { notes: await retrieveNotes(db) });
  });

  app.get('/img/:name', async (req, res) => {
    const stream = await minioClient.getObject(
      minioBucket,
      decodeURIComponent(req.params.name),
    );
    stream.pipe(res);
  });

  app.post(
    '/note',
    multer({ storage: multer.memoryStorage() }).single('image'),
    async (req, res) => {
      if (!req.body.upload && req.body.description) {
        await saveNote(db, { description: req.body.description });
        res.redirect('/');
      } else if (req.body.upload && req.file) {
        await minioClient.putObject(
          minioBucket,
          req.file.originalname,
          req.file.buffer
        );
        const link = `/img/${encodeURIComponent(req.file.originalname)}`;
        res.render('index', {
          content: `${req.body.description} ![](${link})`,
          notes: await retrieveNotes(db),
        });
      }
    }
  );

  app.listen(port, () => {
    console.log(`App listening on http://localhost:${port}`);
  });
}

start()
