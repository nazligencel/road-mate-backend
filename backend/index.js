require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const app = express();
const port = process.env.PORT || 5000;

// Security & Logging Middleware
app.use(helmet()); // Sets various HTTP headers for security
app.use(morgan('dev')); // Logs requests to the console
app.use(cors());
app.use(express.json());

// Database Pool Configuration
const pool = new Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: process.env.DB_PORT,
});

// Test DB Connection & Initialize
const initDb = async () => {
    try {
        const client = await pool.connect();
        console.log('✅ Connected to PostgreSQL');
        client.release();

        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100),
                image VARCHAR(255),
                status VARCHAR(50),
                vehicle VARCHAR(100),
                vehicle_model VARCHAR(100),
                route VARCHAR(100),
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        `);

        // Seed some dummy data if empty
        const { rows } = await pool.query('SELECT COUNT(*) FROM users');
        if (parseInt(rows[0].count) === 0) {
            await pool.query(`
                INSERT INTO users (name, image, status, vehicle, vehicle_model, route, latitude, longitude)
                VALUES 
                ('Selin', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&q=80', 'Şu an çevrimiçi', '4x4 Off-road', 'VW Transporter T4', 'Kuzey (Akyaka)', 37.0322, 28.3242),
                ('Jax', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&q=80', 'Çevrimdışı', 'Ford Transit', 'Ford Transit Custom', 'Güney (Kaş)', 37.0422, 28.3142),
                ('Sage', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&q=80', 'Şu an çevrimiçi', 'Vanagon', 'VW Westfalia', 'Batı (Urla)', 37.0222, 28.3342);
            `);
            console.log('🌱 Dummy data seeded');
        }
    } catch (err) {
        console.error('❌ Database error:', err);
    }
};

initDb();

// --- API Routes ---

app.get('/', (req, res) => {
    res.json({ message: 'RoadMate API is running', version: '1.0.0' });
});

// Update user location
app.post('/api/update-location', async (req, res) => {
    const { userId, latitude, longitude } = req.body;

    if (!userId || latitude === undefined || longitude === undefined) {
        return res.status(400).json({ error: 'Missing parameters' });
    }

    try {
        await pool.query(
            'UPDATE users SET latitude = $1, longitude = $2, last_active = CURRENT_TIMESTAMP WHERE id = $3',
            [latitude, longitude, userId]
        );
        res.json({ success: true, timestamp: new Date() });
    } catch (err) {
        console.error('Update Location Error:', err.message);
        res.status(500).json({ error: 'Database update failed' });
    }
});

// Get nearby nomads using Haversine formula
app.get('/api/nearby-nomads', async (req, res) => {
    const { lat, lng } = req.query;

    if (!lat || !lng) {
        return res.status(400).json({ error: 'Latitude and Longitude are required' });
    }

    const query = `
        SELECT *, (
            6371 * acos(
                cos(radians($1)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians($2)) + 
                sin(radians($1)) * sin(radians(latitude))
            )
        ) AS distance
        FROM users
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY distance
        LIMIT 20;
    `;

    try {
        const { rows } = await pool.query(query, [lat, lng]);
        res.json(rows);
    } catch (err) {
        console.error('Fetch Nomads Error:', err.message);
        res.status(500).json({ error: 'Failed to fetch nomads' });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something broke!' });
});

app.listen(port, () => {
    console.log(`🚀 Server ready at http://localhost:${port}`);
});
