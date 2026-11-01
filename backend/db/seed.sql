-- Sample catalogue data. Run in pgAdmin on the 'ebookstore' database AFTER schema.sql.
INSERT INTO categories(name) VALUES ('Fiction'),('Science & Technology'),('Business'),('Self-Help'),('History');
INSERT INTO brands(name) VALUES ('Penguin'),('HarperCollins'),('Oxford Press'),('OReilly'),('Pearson'),('Random House');

INSERT INTO products(title,author,description,price,stock,image_url,delivery_days,category_id,brand_id)
SELECT v.title, v.author, v.descr, v.price, v.stock, 'https://picsum.photos/seed/'||replace(lower(v.title),' ','')||'/300/420', v.dd,
       (SELECT id FROM categories WHERE name=v.cat), (SELECT id FROM brands WHERE name=v.brand)
FROM (VALUES
 ('The Great Gatsby','F. Scott Fitzgerald','A story of wealth and longing in 1920s America.',299.00,25,3,'Fiction','Penguin'),
 ('1984','George Orwell','A dystopian novel about surveillance and control.',349.00,30,3,'Fiction','Penguin'),
 ('To Kill a Mockingbird','Harper Lee','A classic about justice and growing up in the American South.',399.00,20,4,'Fiction','HarperCollins'),
 ('Pride and Prejudice','Jane Austen','A witty romance of manners and misunderstandings.',249.00,18,4,'Fiction','Random House'),
 ('Clean Code','Robert C. Martin','A handbook of agile software craftsmanship.',899.00,15,5,'Science & Technology','Pearson'),
 ('Designing Data-Intensive Applications','Martin Kleppmann','The big ideas behind reliable, scalable systems.',1299.00,12,6,'Science & Technology','OReilly'),
 ('A Brief History of Time','Stephen Hawking','From the Big Bang to black holes.',459.00,22,4,'Science & Technology','Random House'),
 ('Learning Spring Boot','Greg L. Turnquist','Build production-ready Java services.',799.00,10,6,'Science & Technology','OReilly'),
 ('The Lean Startup','Eric Ries','How constant innovation creates successful businesses.',549.00,17,5,'Business','Penguin'),
 ('Zero to One','Peter Thiel','Notes on startups, or how to build the future.',499.00,14,5,'Business','Random House'),
 ('Atomic Habits','James Clear','Tiny changes, remarkable results.',599.00,40,3,'Self-Help','Random House'),
 ('Deep Work','Cal Newport','Rules for focused success in a distracted world.',529.00,26,4,'Self-Help','Penguin'),
 ('Sapiens','Yuval Noah Harari','A brief history of humankind.',649.00,28,4,'History','HarperCollins'),
 ('Guns, Germs, and Steel','Jared Diamond','The fates of human societies.',579.00,16,5,'History','Oxford Press')
) AS v(title,author,descr,price,stock,dd,cat,brand);

-- A few hand-picked related-book links (both directions)
INSERT INTO related_products(product_id, related_id)
SELECT a.id, b.id FROM products a, products b WHERE
 (a.title='Clean Code' AND b.title IN ('Designing Data-Intensive Applications','Learning Spring Boot')) OR
 (a.title='Designing Data-Intensive Applications' AND b.title IN ('Clean Code','Learning Spring Boot')) OR
 (a.title='Atomic Habits' AND b.title='Deep Work') OR
 (a.title='Deep Work' AND b.title='Atomic Habits') OR
 (a.title='1984' AND b.title IN ('The Great Gatsby','To Kill a Mockingbird'));
