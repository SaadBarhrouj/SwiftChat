-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 03 avr. 2025 à 02:21
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `swiftchat`
--

-- --------------------------------------------------------

--
-- Structure de la table `contacts`
--

CREATE TABLE `contacts` (
                            `contact_id` int(11) NOT NULL,
                            `user_id` int(11) NOT NULL,
                            `contact_user_id` int(11) NOT NULL,
                            `nickname` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `contacts`
--

INSERT INTO `contacts` (`contact_id`, `user_id`, `contact_user_id`, `nickname`) VALUES
                                                                                    (4, 5, 4, 'saad'),
                                                                                    (5, 4, 5, 'kaoutar');

-- --------------------------------------------------------

--
-- Structure de la table `groupe`
--

CREATE TABLE `groupe` (
                          `Groupe_id` int(11) NOT NULL,
                          `Groupe_name` varchar(255) NOT NULL,
                          `Groupe_description` varchar(255) DEFAULT NULL,
                          `Groupe_admin_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `groupe`
--

INSERT INTO `groupe` (`Groupe_id`, `Groupe_name`, `Groupe_description`, `Groupe_admin_id`) VALUES
    (1, 'saad', 'saaaaaaaaaaaaaaaad', 4);

-- --------------------------------------------------------

--
-- Structure de la table `messages`
--

CREATE TABLE `messages` (
                            `message_id` int(11) NOT NULL,
                            `sender_id` int(11) DEFAULT NULL,
                            `receiver_id` int(11) DEFAULT NULL,
                            `group_id` int(11) DEFAULT NULL,
                            `message` text NOT NULL,
                            `messageType` varchar(10) DEFAULT 'text',
                            `fileName` varchar(255) DEFAULT NULL,
                            `date` datetime NOT NULL,
                            `is_read` tinyint(1) DEFAULT 0,
                            `is_deleted` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages`
--

INSERT INTO `messages` (`message_id`, `sender_id`, `receiver_id`, `group_id`, `message`, `messageType`, `fileName`, `date`, `is_read`, `is_deleted`) VALUES
                                                                                                                                                         (11, 5, 4, NULL, 'salam', 'text', NULL, '2025-04-01 15:29:31', 0, 0),
                                                                                                                                                         (12, 4, 5, NULL, 'salam', 'text', NULL, '2025-04-01 15:30:34', 0, 0),
                                                                                                                                                         (13, 4, 5, NULL, 'cc', 'text', NULL, '2025-04-01 15:30:43', 0, 0),
                                                                                                                                                         (14, 4, 5, NULL, 'aaaaaaaaaaaaaaaaaaaaaa', 'text', NULL, '2025-04-01 15:30:50', 0, 0),
                                                                                                                                                         (15, 5, 4, NULL, 'ssssssssssssssssssssssssssss', 'text', NULL, '2025-04-01 15:31:00', 0, 0),
                                                                                                                                                         (16, 4, 5, NULL, 'cccc', 'text', NULL, '2025-04-01 15:48:01', 0, 0),
                                                                                                                                                         (17, 4, 5, NULL, 'ccc', 'text', NULL, '2025-04-01 15:48:21', 0, 0),
                                                                                                                                                         (18, 4, 5, NULL, 'cc', 'text', NULL, '2025-04-01 15:48:25', 0, 0),
                                                                                                                                                         (19, 4, 5, NULL, 'bbbbbbb', 'text', NULL, '2025-04-01 15:49:37', 0, 0),
                                                                                                                                                         (20, 4, 5, NULL, ';;;;;;lllllll', 'text', NULL, '2025-04-01 15:49:52', 0, 0),
                                                                                                                                                         (21, 4, 5, NULL, 'sss', 'text', NULL, '2025-04-01 16:06:24', 0, 0),
                                                                                                                                                         (22, 4, 5, NULL, 'sssssssss', 'text', NULL, '2025-04-01 16:06:34', 0, 0),
                                                                                                                                                         (23, 4, 5, NULL, 'sssssssss', 'text', NULL, '2025-04-01 16:06:39', 0, 0),
                                                                                                                                                         (24, 4, 5, NULL, 'kkk', 'text', NULL, '2025-04-01 16:10:38', 0, 0),
                                                                                                                                                         (25, 4, 5, NULL, 'hhhhhhh', 'text', NULL, '2025-04-01 16:44:03', 0, 0),
                                                                                                                                                         (26, 4, 5, NULL, 'hhhhh', 'text', NULL, '2025-04-01 16:44:07', 0, 0),
                                                                                                                                                         (27, 4, 5, NULL, 'kkkkk', 'text', NULL, '2025-04-01 16:44:11', 0, 0),
                                                                                                                                                         (28, 4, 5, NULL, 'eeeeeeeeeee', 'text', NULL, '2025-04-01 16:44:28', 0, 0),
                                                                                                                                                         (29, 4, 5, NULL, 'eeeee', 'text', NULL, '2025-04-01 16:44:32', 0, 0),
                                                                                                                                                         (30, 4, 5, NULL, 'eeeee', 'text', NULL, '2025-04-01 16:44:35', 0, 0),
                                                                                                                                                         (31, 4, 5, NULL, 'eeeee', 'text', NULL, '2025-04-01 16:44:37', 0, 0),
                                                                                                                                                         (32, 4, 5, NULL, 'salam', 'text', NULL, '2025-04-01 16:44:42', 0, 0),
                                                                                                                                                         (33, 4, 5, NULL, 'ddddddddd', 'text', NULL, '2025-04-01 16:44:53', 0, 0),
                                                                                                                                                         (34, 4, 5, NULL, 'dddd', 'text', NULL, '2025-04-01 16:44:55', 0, 0),
                                                                                                                                                         (35, 4, 5, NULL, 'dddd', 'text', NULL, '2025-04-01 16:44:57', 0, 0),
                                                                                                                                                         (36, 4, 5, NULL, 'exit', 'text', NULL, '2025-04-01 16:45:20', 0, 0),
                                                                                                                                                         (37, 4, 5, NULL, 'exit', 'text', NULL, '2025-04-01 16:45:22', 0, 0),
                                                                                                                                                         (38, 4, 5, NULL, 'quitter', 'text', NULL, '2025-04-01 16:45:26', 0, 0),
                                                                                                                                                         (39, 5, 4, NULL, 'salam', 'text', NULL, '2025-04-01 16:51:59', 0, 0),
                                                                                                                                                         (40, 4, 5, NULL, 'ssss', 'text', NULL, '2025-04-01 16:52:09', 0, 0),
                                                                                                                                                         (41, 5, 4, NULL, 'sssssssss', 'text', NULL, '2025-04-01 16:52:24', 0, 0),
                                                                                                                                                         (42, 4, 5, NULL, 'llll', 'text', NULL, '2025-04-01 16:53:08', 0, 0),
                                                                                                                                                         (43, 4, 5, NULL, 'llll', 'text', NULL, '2025-04-01 16:53:32', 0, 0),
                                                                                                                                                         (44, 4, 5, NULL, 'kkk', 'text', NULL, '2025-04-01 16:53:38', 0, 0),
                                                                                                                                                         (45, 5, 4, NULL, '[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D', 'text', NULL, '2025-04-01 16:53:53', 0, 0),
                                                                                                                                                         (46, 4, 5, NULL, 'salam', 'text', NULL, '2025-04-01 17:30:34', 0, 0),
                                                                                                                                                         (47, 4, 5, NULL, 'cv', 'text', NULL, '2025-04-01 17:30:40', 0, 0),
                                                                                                                                                         (48, 4, 5, NULL, 'cv', 'text', NULL, '2025-04-01 17:30:43', 0, 0),
                                                                                                                                                         (49, 4, 5, NULL, 'ddd', 'text', NULL, '2025-04-01 17:30:51', 0, 0),
                                                                                                                                                         (50, 5, 4, NULL, 'cv', 'text', NULL, '2025-04-01 17:32:08', 0, 0),
                                                                                                                                                         (51, 5, 4, NULL, 'fffffff', 'text', NULL, '2025-04-01 17:32:25', 0, 0),
                                                                                                                                                         (52, 4, 5, NULL, 'hhhh', 'text', NULL, '2025-04-01 17:32:32', 0, 0),
                                                                                                                                                         (53, 5, 4, NULL, 'cccccccc', 'text', NULL, '2025-04-01 17:33:17', 0, 0),
                                                                                                                                                         (54, 5, 4, NULL, 'ggggg', 'text', NULL, '2025-04-01 17:34:02', 0, 0),
                                                                                                                                                         (55, 5, 4, NULL, 'hhhhh', 'text', NULL, '2025-04-01 17:35:08', 0, 0),
                                                                                                                                                         (56, 4, 5, NULL, 'llllllll', 'text', NULL, '2025-04-01 17:35:30', 0, 0),
                                                                                                                                                         (57, 5, 4, NULL, 'fff', 'text', NULL, '2025-04-01 17:36:31', 0, 0),
                                                                                                                                                         (58, 5, 4, NULL, 'ffffffff', 'text', NULL, '2025-04-01 17:36:33', 0, 0),
                                                                                                                                                         (59, 5, 4, NULL, 'fffffff', 'text', NULL, '2025-04-01 17:36:37', 0, 0),
                                                                                                                                                         (60, 4, 5, NULL, 'hhhh', 'text', NULL, '2025-04-02 00:20:23', 0, 0),
                                                                                                                                                         (61, 4, 5, NULL, 'hhhhh', 'text', NULL, '2025-04-02 00:20:27', 0, 0),
                                                                                                                                                         (62, 4, 5, NULL, 'hhhhhhhhhhhhhhh', 'text', NULL, '2025-04-02 00:20:31', 0, 0),
                                                                                                                                                         (63, 4, 5, NULL, 'hhhhhhhhhhhh', 'text', NULL, '2025-04-02 00:20:38', 0, 0),
                                                                                                                                                         (64, 4, NULL, 1, 'saaaae', 'text', NULL, '2025-04-02 00:57:39', 0, 0),
                                                                                                                                                         (65, 4, NULL, 1, 'hiii', 'text', NULL, '2025-04-02 05:51:11', 0, 0),
                                                                                                                                                         (66, 4, NULL, 1, 'ssssss', 'text', NULL, '2025-04-02 05:51:15', 0, 0),
                                                                                                                                                         (67, 4, NULL, 1, 'sss', 'text', NULL, '2025-04-02 05:51:17', 0, 0),
                                                                                                                                                         (68, 5, NULL, 1, 'hi', 'text', NULL, '2025-04-02 05:52:03', 0, 0),
                                                                                                                                                         (69, 5, NULL, 1, 'hiiiiiii', 'text', NULL, '2025-04-02 05:52:11', 0, 0),
                                                                                                                                                         (70, 4, NULL, 1, 'hhhh', 'text', NULL, '2025-04-02 05:52:17', 0, 0),
                                                                                                                                                         (71, 6, NULL, 1, 'hi', 'text', NULL, '2025-04-02 06:05:02', 0, 0),
                                                                                                                                                         (72, 5, NULL, 1, 'hi', 'text', NULL, '2025-04-02 06:05:31', 0, 0),
                                                                                                                                                         (73, 6, NULL, 1, 'hi', 'text', NULL, '2025-04-02 06:05:49', 0, 0),
                                                                                                                                                         (74, 5, NULL, 1, 'hi', 'text', NULL, '2025-04-02 06:06:06', 0, 0),
                                                                                                                                                         (75, 4, 5, NULL, 'upload', 'text', NULL, '2025-04-02 06:36:02', 0, 0),
                                                                                                                                                         (76, 4, 5, NULL, 'upload C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\uploads\\test.txt', 'text', NULL, '2025-04-02 06:36:56', 0, 0),
                                                                                                                                                         (77, 4, 5, NULL, 'upload C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\uploads\\test.txt', 'text', NULL, '2025-04-02 06:39:59', 0, 0),
                                                                                                                                                         (78, 4, 5, NULL, 'uploadC:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\uploads', 'text', NULL, '2025-04-02 06:40:13', 0, 0),
                                                                                                                                                         (79, 4, 5, NULL, 'upload', 'text', NULL, '2025-04-02 06:40:20', 0, 0),
                                                                                                                                                         (80, 4, 5, NULL, '', 'file', 'test.txt', '2025-04-02 06:49:22', 0, 0),
                                                                                                                                                         (81, 5, 4, NULL, '', 'file', 'test2.txt', '2025-04-02 06:51:33', 0, 0),
                                                                                                                                                         (82, 4, 5, NULL, 'upload C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\utils\\test\\test2.txt', 'text', NULL, '2025-04-02 06:52:44', 0, 0),
                                                                                                                                                         (83, 5, 4, NULL, '', 'file', 'test2.txt', '2025-04-02 06:53:14', 0, 0),
                                                                                                                                                         (84, 5, 4, NULL, '', 'file', 'test.png', '2025-04-02 06:54:47', 0, 0),
                                                                                                                                                         (85, 4, 5, NULL, 'salam kaoutar', 'text', NULL, '2025-04-02 14:20:03', 0, 0),
                                                                                                                                                         (86, 5, 4, NULL, 'ccccccccccc', 'text', NULL, '2025-04-02 14:20:18', 0, 0),
                                                                                                                                                         (87, 4, 5, NULL, '', 'file', 'test2.txt', '2025-04-02 14:21:34', 0, 0),
                                                                                                                                                         (88, 5, 4, NULL, 'ssssss', 'text', NULL, '2025-04-02 14:22:03', 0, 0),
                                                                                                                                                         (89, 4, 5, NULL, 'retouar', 'text', NULL, '2025-04-02 14:23:01', 0, 0),
                                                                                                                                                         (90, 4, NULL, 1, 'salam', 'text', NULL, '2025-04-02 14:23:44', 0, 0),
                                                                                                                                                         (91, 5, NULL, 1, 'cccccccccccccccccccccccccccccccc', 'text', NULL, '2025-04-02 14:24:11', 0, 0),
                                                                                                                                                         (92, 4, NULL, 1, 'fin', 'text', NULL, '2025-04-02 14:24:15', 0, 0),
                                                                                                                                                         (93, 4, 5, NULL, '', 'file', 'test.png', '2025-04-02 23:17:05', 0, 0),
                                                                                                                                                         (94, 4, 5, NULL, 'viewview', 'text', NULL, '2025-04-02 23:17:36', 0, 0),
                                                                                                                                                         (95, 4, 5, NULL, 'view', 'text', NULL, '2025-04-02 23:17:40', 0, 0),
                                                                                                                                                         (96, 5, 4, NULL, 'view', 'text', NULL, '2025-04-02 23:18:12', 0, 0),
                                                                                                                                                         (97, 4, 5, NULL, '', 'file', 'test.png', '2025-04-02 23:29:20', 0, 0),
                                                                                                                                                         (98, 4, 5, NULL, 'view', 'text', NULL, '2025-04-02 23:30:26', 0, 0),
                                                                                                                                                         (99, 4, 5, NULL, 'vie', 'text', NULL, '2025-04-02 23:41:29', 0, 0),
                                                                                                                                                         (100, 4, 5, NULL, 'C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\utils\\testz', 'text', NULL, '2025-04-02 23:42:17', 0, 0),
                                                                                                                                                         (101, 4, 5, NULL, '', 'file', 'test.pdf', '2025-04-02 23:42:28', 0, 0),
                                                                                                                                                         (102, 4, 5, NULL, '', 'file', 'test.png', '2025-04-02 23:42:41', 0, 0),
                                                                                                                                                         (103, 4, 5, NULL, '', 'file', 'test.txt', '2025-04-02 23:42:51', 0, 0),
                                                                                                                                                         (104, 5, 4, NULL, '', 'file', 'test.sql', '2025-04-02 23:47:03', 0, 0),
                                                                                                                                                         (105, 5, 4, NULL, 'C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\utils\\test[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D[D', 'text', NULL, '2025-04-02 23:47:34', 0, 0),
                                                                                                                                                         (106, 5, 4, NULL, 'upload C:\\Users\\Lenovo\\Desktop\\Swiftchat-cmd\\src\\utils\\test\\test.wav', 'text', NULL, '2025-04-02 23:47:44', 0, 0),
                                                                                                                                                         (107, 5, 4, NULL, '', 'file', 'test.wav', '2025-04-02 23:47:51', 0, 0),
                                                                                                                                                         (108, 5, 4, NULL, '', 'file', 'test.wav', '2025-04-02 23:49:51', 0, 0),
                                                                                                                                                         (109, 5, 4, NULL, 'viw 107', 'text', NULL, '2025-04-02 23:49:57', 0, 0),
                                                                                                                                                         (110, 5, 4, NULL, 'viw', 'text', NULL, '2025-04-02 23:50:01', 0, 0),
                                                                                                                                                         (111, 5, 4, NULL, '', 'file', 'test.mp4', '2025-04-02 23:50:36', 0, 0),
                                                                                                                                                         (112, 5, 4, NULL, '', 'file', 'test.mp4', '2025-04-02 23:51:52', 0, 0),
                                                                                                                                                         (113, 4, 5, NULL, '', 'file', 'test.pdf', '2025-04-03 00:06:21', 0, 0),
                                                                                                                                                         (114, 4, 5, NULL, '', 'file', '113_test.pdf', '2025-04-03 00:08:19', 0, 0),
                                                                                                                                                         (115, 4, 5, NULL, '', 'file', 'test.mp4', '2025-04-03 00:08:54', 0, 0),
                                                                                                                                                         (116, 4, 5, NULL, 'sss', 'text', NULL, '2025-04-03 02:19:15', 0, 0),
                                                                                                                                                         (117, 4, NULL, 1, 'help', 'text', NULL, '2025-04-03 02:19:43', 0, 0),
                                                                                                                                                         (118, 4, NULL, 1, 'upload', 'text', NULL, '2025-04-03 02:19:47', 0, 0);

-- --------------------------------------------------------

--
-- Structure de la table `pending_messages`
--

CREATE TABLE `pending_messages` (
                                    `pending_id` int(11) NOT NULL,
                                    `user_id` int(11) NOT NULL,
                                    `message_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `pending_messages`
--

INSERT INTO `pending_messages` (`pending_id`, `user_id`, `message_id`) VALUES
                                                                           (43, 6, 90),
                                                                           (44, 6, 91),
                                                                           (45, 6, 92),
                                                                           (57, 5, 113),
                                                                           (58, 5, 114),
                                                                           (59, 5, 116),
                                                                           (60, 5, 117),
                                                                           (61, 6, 117),
                                                                           (62, 5, 118),
                                                                           (63, 6, 118);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
                         `user_id` int(11) NOT NULL,
                         `email` varchar(255) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `name` varchar(255) NOT NULL,
                         `is_online` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`user_id`, `email`, `password`, `name`, `is_online`) VALUES
                                                                              (4, 'saad@gmail.com', 'Sadd2001', 'saad', 0),
                                                                              (5, 'kaoutar@ga.com', 'Sadd2001', 'kaoutar', 1),
                                                                              (6, 'simo@gmail.com', 'Sadd2001', 'simo', 0);

-- --------------------------------------------------------

--
-- Structure de la table `users_groups`
--

CREATE TABLE `users_groups` (
                                `user_id` int(11) NOT NULL,
                                `group_id` int(11) NOT NULL,
                                `joined_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users_groups`
--

INSERT INTO `users_groups` (`user_id`, `group_id`, `joined_at`) VALUES
                                                                    (4, 1, '2025-04-02 00:56:50'),
                                                                    (5, 1, '2025-04-02 00:57:22'),
                                                                    (6, 1, '2025-04-02 06:04:29');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `contacts`
--
ALTER TABLE `contacts`
    ADD PRIMARY KEY (`contact_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `contact_user_id` (`contact_user_id`);

--
-- Index pour la table `groupe`
--
ALTER TABLE `groupe`
    ADD PRIMARY KEY (`Groupe_id`),
  ADD KEY `Groupe_admin_id` (`Groupe_admin_id`);

--
-- Index pour la table `messages`
--
ALTER TABLE `messages`
    ADD PRIMARY KEY (`message_id`),
  ADD KEY `sender_id` (`sender_id`),
  ADD KEY `receiver_id` (`receiver_id`),
  ADD KEY `group_id` (`group_id`);

--
-- Index pour la table `pending_messages`
--
ALTER TABLE `pending_messages`
    ADD PRIMARY KEY (`pending_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `message_id` (`message_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
    ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Index pour la table `users_groups`
--
ALTER TABLE `users_groups`
    ADD PRIMARY KEY (`user_id`,`group_id`),
  ADD KEY `group_id` (`group_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `contacts`
--
ALTER TABLE `contacts`
    MODIFY `contact_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `groupe`
--
ALTER TABLE `groupe`
    MODIFY `Groupe_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `messages`
--
ALTER TABLE `messages`
    MODIFY `message_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=119;

--
-- AUTO_INCREMENT pour la table `pending_messages`
--
ALTER TABLE `pending_messages`
    MODIFY `pending_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
    MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `contacts`
--
ALTER TABLE `contacts`
    ADD CONSTRAINT `contacts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `contacts_ibfk_2` FOREIGN KEY (`contact_user_id`) REFERENCES `users` (`user_id`);

--
-- Contraintes pour la table `groupe`
--
ALTER TABLE `groupe`
    ADD CONSTRAINT `groupe_ibfk_1` FOREIGN KEY (`Groupe_admin_id`) REFERENCES `users` (`user_id`);

--
-- Contraintes pour la table `messages`
--
ALTER TABLE `messages`
    ADD CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `messages_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `groupe` (`Groupe_id`);

--
-- Contraintes pour la table `pending_messages`
--
ALTER TABLE `pending_messages`
    ADD CONSTRAINT `pending_messages_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `pending_messages_ibfk_2` FOREIGN KEY (`message_id`) REFERENCES `messages` (`message_id`);

--
-- Contraintes pour la table `users_groups`
--
ALTER TABLE `users_groups`
    ADD CONSTRAINT `users_groups_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `users_groups_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groupe` (`Groupe_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
