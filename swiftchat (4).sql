-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1:3306
-- Généré le : sam. 08 mars 2025 à 21:40
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
(1, 2, 1, 'saad'),
(2, 1, 2, 'kaataar');

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

-- --------------------------------------------------------

--
-- Structure de la table `group_history`
--

CREATE TABLE `group_history` (
  `history_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `event_type` varchar(255) NOT NULL,
  `event_details` text DEFAULT NULL,
  `event_date` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `group_roles`
--

CREATE TABLE `group_roles` (
  `role_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `role` enum('admin','member') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
(1, 2, 1, NULL, 'salam', 'text', NULL, '2025-03-07 04:36:54', 0, 0),
(2, 2, 1, NULL, 'salam', 'text', NULL, '2025-03-08 04:50:09', 0, 0),
(3, 2, 1, NULL, 'salam', 'text', NULL, '2025-03-08 04:51:43', 0, 0),
(4, 2, 1, NULL, 'saaa', 'text', NULL, '2025-03-08 04:52:01', 0, 0),
(5, 1, 2, NULL, 'salam', 'text', NULL, '2025-03-08 04:53:21', 0, 0),
(6, 1, 2, NULL, 'salam', 'text', NULL, '2025-03-08 05:13:16', 0, 0),
(7, 2, 1, NULL, '1234', 'text', NULL, '2025-03-08 05:13:40', 0, 0),
(8, 1, 2, NULL, 'salam', 'text', NULL, '2025-03-08 05:27:12', 0, 0),
(9, 1, 2, NULL, 'salam', 'text', NULL, '2025-03-08 06:50:13', 0, 0),
(10, 2, 1, NULL, 'saaad cv', 'text', NULL, '2025-03-08 06:57:24', 0, 0);

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
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 2, 5),
(6, 2, 6),
(7, 1, 7),
(8, 2, 8),
(9, 2, 9),
(10, 1, 10);

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
(1, 'saad@gmail.com', 'Saad1234', 'saad', 1),
(2, 'kaoutar@gamil.com', 'Kaoutar1234', 'kaoutarr', 1);

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
-- Index pour la table `group_history`
--
ALTER TABLE `group_history`
  ADD PRIMARY KEY (`history_id`),
  ADD KEY `group_id` (`group_id`);

--
-- Index pour la table `group_roles`
--
ALTER TABLE `group_roles`
  ADD PRIMARY KEY (`role_id`),
  ADD KEY `group_id` (`group_id`),
  ADD KEY `user_id` (`user_id`);

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
  MODIFY `contact_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `groupe`
--
ALTER TABLE `groupe`
  MODIFY `Groupe_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `group_history`
--
ALTER TABLE `group_history`
  MODIFY `history_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `group_roles`
--
ALTER TABLE `group_roles`
  MODIFY `role_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `messages`
--
ALTER TABLE `messages`
  MODIFY `message_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `pending_messages`
--
ALTER TABLE `pending_messages`
  MODIFY `pending_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

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
-- Contraintes pour la table `group_history`
--
ALTER TABLE `group_history`
  ADD CONSTRAINT `group_history_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groupe` (`Groupe_id`);

--
-- Contraintes pour la table `group_roles`
--
ALTER TABLE `group_roles`
  ADD CONSTRAINT `group_roles_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groupe` (`Groupe_id`),
  ADD CONSTRAINT `group_roles_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

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
